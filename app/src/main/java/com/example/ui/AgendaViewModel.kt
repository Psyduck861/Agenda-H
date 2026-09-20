package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab {
    INICIO, MULHERES, AGENDA, STATS, CONFIG, GPS, DATE_SUGGESTIONS, FICHAS, COACH
}

enum class PartnerFilter {
    TODOS, ATIVAS, SEM_SEXO, TRANSADAS, INATIVAS, SEM_FILHOS, FAZ_ANAL
}

enum class PartnerSortOption {
    ABSTINENCIA, RANKING, NOME
}

enum class StatsFilter {
    GERAL, ANUAL, RANKINGS
}

class AgendaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AgendaRepository(database.partnerDao(), database.encontroDao())

    // SharedPreferences for Theme and Security Settings
    private val prefs = application.getSharedPreferences("agenda_h_prefs", android.content.Context.MODE_PRIVATE)

    var isDarkTheme by mutableStateOf(prefs.getBoolean("dark_theme", true))
        private set

    var pinCode by mutableStateOf(prefs.getString("pin_code", "") ?: "")
        private set

    var isBiometricsEnabled by mutableStateOf(prefs.getBoolean("biometrics_enabled", false))
        private set

    var isAppLocked by mutableStateOf((prefs.getString("pin_code", "") ?: "").isNotEmpty())

    fun setDarkThemeEnabled(enabled: Boolean) {
        isDarkTheme = enabled
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    fun setPinCodeValue(pin: String) {
        pinCode = pin
        prefs.edit().putString("pin_code", pin).apply()
        if (pin.isEmpty()) {
            isAppLocked = false
            setBiometricsEnabledValue(false)
        }
    }

    fun setBiometricsEnabledValue(enabled: Boolean) {
        isBiometricsEnabled = enabled
        prefs.edit().putBoolean("biometrics_enabled", enabled).apply()
    }

    // Tabs Navigation State
    var currentTab by mutableStateOf(AppTab.INICIO)

    // Search and Filter States
    var partnerSearchQuery by mutableStateOf("")
    var gpSearchQuery by mutableStateOf("")
    var activePartnerFilter by mutableStateOf(PartnerFilter.TODOS)
    private var _partnerSortOption by mutableStateOf(
        try {
            val savedName = prefs.getString("partner_sort_option", PartnerSortOption.ABSTINENCIA.name)
            PartnerSortOption.valueOf(savedName ?: PartnerSortOption.ABSTINENCIA.name)
        } catch (e: Exception) {
            PartnerSortOption.ABSTINENCIA
        }
    )
    var partnerSortOption: PartnerSortOption
        get() = _partnerSortOption
        set(value) {
            _partnerSortOption = value
            prefs.edit().putString("partner_sort_option", value.name).apply()
        }
    var activeStatsFilter by mutableStateOf(StatsFilter.GERAL)
    var abstinenceFilter by mutableStateOf("Total") // "Total", "Sem GP", "Só GP"

    // Privacy Switcher State
    var mostrarGP by mutableStateOf(prefs.getBoolean("mostrar_gp_toggle", true))
        private set

    fun setMostrarGPValue(value: Boolean) {
        mostrarGP = value
        prefs.edit().putBoolean("mostrar_gp_toggle", value).apply()
        if (!value && abstinenceFilter == "Só GP") {
            abstinenceFilter = "Total"
        }
    }

    var isFloatingCoachBubbleEnabled by mutableStateOf(false)
        private set

    fun updateFloatingCoachBubble(enabled: Boolean) {
        isFloatingCoachBubbleEnabled = enabled
        prefs.edit().putBoolean("floating_coach_bubble", enabled).apply()
    }

    // Navigation cache for Date Suggestions
    var destinationPartnerId by mutableStateOf<Int?>(null)
    var destinationLocationFilter by mutableStateOf<String>("Minha localização")

    fun navigateToDateSuggestions(partnerId: Int, initialMode: String = "Casa dela") {
        destinationPartnerId = partnerId
        destinationLocationFilter = initialMode
        currentTab = AppTab.DATE_SUGGESTIONS
    }

    // Location Service integration & Realtime GPS
    val locationService = LocationService()

    var userGpsLatitude by mutableStateOf(-22.9064)
    var userGpsLongitude by mutableStateOf(-47.0616)
    var userGpsAddress by mutableStateOf("Campinas, SP")

    fun updateUserGps(lat: Double, lng: Double, address: String) {
        userGpsLatitude = lat
        userGpsLongitude = lng
        userGpsAddress = address
    }

    private val _nearbySuggestions = MutableStateFlow<List<LocationService.NearbyPlace>>(emptyList())
    val nearbySuggestions: StateFlow<List<LocationService.NearbyPlace>> = _nearbySuggestions.asStateFlow()

    private val _isLoadingSuggestions = MutableStateFlow(false)
    val isLoadingSuggestions: StateFlow<Boolean> = _isLoadingSuggestions.asStateFlow()

    // Smart Date AI using Gemini flash-latest
    var smartDateAiResult by mutableStateOf("")
    var isSmartDateAiLoading by mutableStateOf(false)

    fun loadNearbySuggestions(mode: String, partnerId: String?, category: String, radiusInMeters: Int = 7000) {
        viewModelScope.launch {
            _isLoadingSuggestions.value = true
            try {
                // Strictly enforce maximum 7000m (7km) radius
                val strictRadius = minOf(radiusInMeters, 7000)
                val origin = calculateDateOrigin(mode, partnerId)
                val places = locationService.fetchNearbyDates(
                    latitude = origin.first,
                    longitude = origin.second,
                    radiusInMeters = strictRadius,
                    category = category
                )
                // Filter ensuring strictly <= 7.0 km
                _nearbySuggestions.value = places.filter { it.distanceInKm <= 7.0 }
            } catch (e: Exception) {
                _nearbySuggestions.value = emptyList()
            } finally {
                _isLoadingSuggestions.value = false
            }
        }
    }

    suspend fun calculateDateOrigin(mode: String, partnerId: String?): Pair<Double, Double> = withContext(Dispatchers.IO) {
        // Base Location: Realtime user GPS coordinates
        val myLat = userGpsLatitude
        val myLng = userGpsLongitude
        
        when (mode) {
            "Minha localização" -> Pair(myLat, myLng)
            "Casa dela" -> {
                val partnerIdInt = partnerId?.toIntOrNull()
                if (partnerIdInt != null) {
                    val partner = repository.getPartnerById(partnerIdInt)
                    if (partner != null && partner.address.isNotEmpty()) {
                        geocodeAddress(partner.address)
                    } else {
                        Pair(myLat, myLng)
                    }
                } else {
                    Pair(myLat, myLng)
                }
            }
            else -> Pair(myLat, myLng)
        }
    }

    private fun geocodeAddress(address: String): Pair<Double, Double> {
        val lowercaseAddr = address.lowercase()
        return when {
            lowercaseAddr.contains("campinas") -> {
                Pair(-22.8942, -47.0512)
            }
            lowercaseAddr.contains("santos") -> {
                Pair(-23.9682, -46.3339)
            }
            lowercaseAddr.contains("rj") || lowercaseAddr.contains("rio") -> {
                Pair(-22.9068, -43.1729)
            }
            lowercaseAddr.contains("paulista") || lowercaseAddr.contains("são paulo") || lowercaseAddr.contains("sp") -> {
                Pair(-23.5505, -46.6333)
            }
            else -> {
                val hash = address.hashCode()
                val latOffset = (hash % 100) / 1000.0
                val lngOffset = ((hash / 100) % 100) / 1000.0
                Pair(-23.5505 + latOffset, -46.6333 + lngOffset)
            }
        }
    }

    // Dialog state controllers
    var showAddPartnerDialog by mutableStateOf(false)
    var showAddGPDialog by mutableStateOf(false)
    var showAddEncontroDialog by mutableStateOf(false)
    var preselectedPartnerIdForEncontro by mutableStateOf(0)
    var selectedSubTab by mutableStateOf(0) // 0 = Mulheres, 1 = GPs
    var partnerToEdit by mutableStateOf<Partner?>(null)
    var gpToEdit by mutableStateOf<Partner?>(null)

    // Live Flow data from SQLite Room
    val allPartners: StateFlow<List<Partner>> = repository.allPartners
        .map { list ->
            list.map { p ->
                p.copy(
                    name = p.name ?: "",
                    location = p.location ?: "",
                    origin = p.origin ?: "",
                    photoUrl = p.photoUrl ?: "",
                    notes = p.notes ?: "",
                    firstDate = p.firstDate ?: "",
                    objective = p.objective ?: "Casual",
                    phone = p.phone ?: "",
                    address = p.address ?: "",
                    availability = p.availability ?: "Solteira",
                    status = p.status ?: "Ativa",
                    negatives = p.negatives ?: ""
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEncontros: StateFlow<List<Encontro>> = repository.allEncontros
        .map { list ->
            list.map { e ->
                e.copy(
                    partnerName = e.partnerName ?: "",
                    date = e.date ?: "",
                    time = e.time ?: "",
                    notes = e.notes ?: ""
                )
            }.sortedByDescending { WomanCalculator.parseDateToMillis(it.date) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var isRestoredOrChecked = false

    // Initialize database default mock visual set if database is fully empty
    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var partners = repository.allPartners.first()
                var encounters = repository.allEncontros.first()
                var restored = false

                // Check if current database only has default mock data (to upgrade if user backup exists)
                val isMockDataOnly = partners.isNotEmpty() && partners.all { p ->
                    listOf("Juliete", "Vanderléia", "Camila", "Larissa", "Bárbara").any { mockName ->
                        p.name.equals(mockName, ignoreCase = true)
                    }
                }
                
                if (partners.isEmpty() || encounters.isEmpty() || isMockDataOnly) {
                    // Priority 1: Check Agenda H folder on device storage (the user's real backup file)
                    val (folderBackup, path) = com.example.data.AgendaHFolderManager.readBackupFromAgendaH(getApplication())
                    if (!folderBackup.isNullOrBlank()) {
                        restored = parseAndImportJsonString(folderBackup)
                        if (restored) {
                            android.util.Log.d("AgendaViewModel", "Backup restaurado com sucesso da pasta Agenda H: $path")
                        }
                    }

                    // Priority 2: Check older SQLite databases on disk (e.g. from previous app versions)
                    if (!restored && partners.isEmpty()) {
                        restored = migrateFromOldDatabasesIfPresent()
                    }

                    // Priority 3: SharedPreferences auto_backup_json
                    if (!restored && partners.isEmpty()) {
                        val sharedBackup = prefs.getString("auto_backup_json", "") ?: ""
                        if (sharedBackup.isNotEmpty()) {
                            restored = restoreBackup(sharedBackup)
                        }
                    }

                    // Priority 4: Internal filesDir backup files
                    if (!restored && partners.isEmpty()) {
                        val backupFile = File(getApplication<Application>().filesDir, "agenda_backup_auto.json")
                        if (backupFile.exists()) {
                            try {
                                val fileBackup = backupFile.readText(Charsets.UTF_8)
                                restored = restoreBackup(fileBackup)
                            } catch (_: Exception) {}
                        }
                    }

                    // Only if completely empty and no backup found anywhere, populate mock data
                    if (!restored && partners.isEmpty()) {
                        repository.populatePredefinedMockData()
                    }
                    
                    // Reload data if restored or populated
                    partners = repository.allPartners.first()
                    encounters = repository.allEncontros.first()
                }
                
                isRestoredOrChecked = true
                
                if (!restored && partners.isNotEmpty() && encounters.isNotEmpty()) {
                    // Retroactively sanitize existing database entries to conform with the new rule
                    encounters.forEach { encounter ->
                        val partner = partners.find { it.id == encounter.partnerId }
                        if (partner != null && !partner.isGP) {
                            val name = partner.name.trim()
                            val isJuliete = name.equals("Juliete", ignoreCase = true)
                            val expectedMotelCost = if (isJuliete) 0.0 else if (encounter.hadSex) 100.0 else 0.0
                            
                            var updatedEncounter = encounter
                            var needsUpdate = false
                            
                            if (encounter.motelCost != expectedMotelCost) {
                                updatedEncounter = updatedEncounter.copy(motelCost = expectedMotelCost)
                                needsUpdate = true
                            }
                            
                            val isAllowed = isJuliete || 
                                            name.equals("Vanderléia", ignoreCase = true) || 
                                            name.equals("Vanderleia", ignoreCase = true)
                            if (!isAllowed && (encounter.typeCreampie || encounter.countCreampie > 0)) {
                                updatedEncounter = updatedEncounter.copy(
                                    typeCreampie = false,
                                    countCreampie = 0
                                )
                                needsUpdate = true
                            }
                            
                            if (needsUpdate) {
                                repository.updateEncontro(updatedEncounter)
                            }
                        }
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            combine(repository.allPartners, repository.allEncontros) { pmList, ecList ->
                Pair(pmList, ecList)
            }.collect { (pm, ec) ->
                if (pm.isNotEmpty()) {
                    saveAutoBackup(pm, ec)
                }
            }
        }
    }

    private suspend fun migrateFromOldDatabasesIfPresent(): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbNames = listOf("agenda_v8_database", "agenda_v7_database", "agenda_database", "agenda_h_database")
            val existingDbs = getApplication<Application>().databaseList() ?: emptyArray()
            for (dbName in dbNames) {
                if (existingDbs.contains(dbName)) {
                    val dbFile = getApplication<Application>().getDatabasePath(dbName)
                    if (dbFile.exists() && dbFile.length() > 0) {
                        val imported = importFromSqliteFile(dbFile)
                        if (imported) return@withContext true
                    }
                }
            }
        } catch (_: Exception) {}
        false
    }

    private suspend fun importFromSqliteFile(dbFile: File): Boolean {
        var db: android.database.sqlite.SQLiteDatabase? = null
        return try {
            db = android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('partners', 'partner')", null)
            val hasPartnerTable = cursor.moveToFirst()
            cursor.close()
            if (!hasPartnerTable) return false

            val pCursor = db.rawQuery("SELECT * FROM partners", null)
            val partnersToInsert = mutableListOf<Partner>()
            val colNames = pCursor.columnNames.toList()
            while (pCursor.moveToNext()) {
                val id = if (colNames.contains("id")) pCursor.getInt(pCursor.getColumnIndexOrThrow("id")) else 0
                val name = if (colNames.contains("name")) pCursor.getString(pCursor.getColumnIndexOrThrow("name")) else ""
                if (name.isBlank()) continue
                val age = if (colNames.contains("age")) pCursor.getInt(pCursor.getColumnIndexOrThrow("age")) else 25
                val status = if (colNames.contains("status")) pCursor.getString(pCursor.getColumnIndexOrThrow("status")) else "Ativa"
                val origin = if (colNames.contains("origin")) pCursor.getString(pCursor.getColumnIndexOrThrow("origin")) else "Tinder"
                val rating = if (colNames.contains("rating")) pCursor.getDouble(pCursor.getColumnIndexOrThrow("rating")) else 5.0
                val address = if (colNames.contains("address")) pCursor.getString(pCursor.getColumnIndexOrThrow("address")) else ""
                val notes = if (colNames.contains("notes")) pCursor.getString(pCursor.getColumnIndexOrThrow("notes")) else ""
                val isGP = if (colNames.contains("isGP")) pCursor.getInt(pCursor.getColumnIndexOrThrow("isGP")) == 1 else false
                val phone = if (colNames.contains("phone")) pCursor.getString(pCursor.getColumnIndexOrThrow("phone")) else ""
                val firstDate = if (colNames.contains("firstDate")) pCursor.getString(pCursor.getColumnIndexOrThrow("firstDate")) else ""
                val objective = if (colNames.contains("objective")) pCursor.getString(pCursor.getColumnIndexOrThrow("objective")) else "Casual"
                val children = if (colNames.contains("children")) pCursor.getInt(pCursor.getColumnIndexOrThrow("children")) else 0
                val myKids = if (colNames.contains("myKids")) pCursor.getInt(pCursor.getColumnIndexOrThrow("myKids")) else 0
                val negatives = if (colNames.contains("negatives")) pCursor.getString(pCursor.getColumnIndexOrThrow("negatives")) else ""
                val gpCost = if (colNames.contains("gpCost")) pCursor.getDouble(pCursor.getColumnIndexOrThrow("gpCost")) else 0.0

                partnersToInsert.add(
                    Partner(
                        id = id,
                        name = name,
                        age = age,
                        status = status,
                        origin = origin,
                        rating = rating,
                        address = address,
                        notes = notes,
                        isGP = isGP,
                        phone = phone,
                        firstDate = firstDate,
                        objective = objective,
                        children = children,
                        myKids = myKids,
                        negatives = negatives,
                        gpPrice = gpCost
                    )
                )
            }
            pCursor.close()

            if (partnersToInsert.isNotEmpty()) {
                partnersToInsert.forEach { repository.insertPartner(it) }
                try {
                    val eCursor = db.rawQuery("SELECT * FROM encontros", null)
                    val eColNames = eCursor.columnNames.toList()
                    while (eCursor.moveToNext()) {
                        val eId = if (eColNames.contains("id")) eCursor.getInt(eCursor.getColumnIndexOrThrow("id")) else 0
                        val partnerId = if (eColNames.contains("partnerId")) eCursor.getInt(eCursor.getColumnIndexOrThrow("partnerId")) else 0
                        val partnerName = if (eColNames.contains("partnerName")) eCursor.getString(eCursor.getColumnIndexOrThrow("partnerName")) else ""
                        val date = if (eColNames.contains("date")) eCursor.getString(eCursor.getColumnIndexOrThrow("date")) else ""
                        val time = if (eColNames.contains("time")) eCursor.getString(eCursor.getColumnIndexOrThrow("time")) else ""
                        val hadSex = if (eColNames.contains("hadSex")) eCursor.getInt(eCursor.getColumnIndexOrThrow("hadSex")) == 1 else false
                        val typeVaginal = if (eColNames.contains("typeVaginal")) eCursor.getInt(eCursor.getColumnIndexOrThrow("typeVaginal")) == 1 else false
                        val typeAnal = if (eColNames.contains("typeAnal")) eCursor.getInt(eCursor.getColumnIndexOrThrow("typeAnal")) == 1 else false
                        val typeOral = if (eColNames.contains("typeOral")) eCursor.getInt(eCursor.getColumnIndexOrThrow("typeOral")) == 1 else false
                        val motelCost = if (eColNames.contains("motelCost")) eCursor.getDouble(eCursor.getColumnIndexOrThrow("motelCost")) else 0.0
                        val notes = if (eColNames.contains("notes")) eCursor.getString(eCursor.getColumnIndexOrThrow("notes")) else ""

                        repository.insertEncontro(
                            Encontro(
                                id = eId,
                                partnerId = partnerId,
                                partnerName = partnerName,
                                date = date,
                                time = time,
                                hadSex = hadSex,
                                typeVaginal = typeVaginal,
                                typeAnal = typeAnal,
                                typeOral = typeOral,
                                motelCost = motelCost,
                                notes = notes
                            )
                        )
                    }
                    eCursor.close()
                } catch (_: Exception) {}
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        } finally {
            db?.close()
        }
    }

    // Filtered partners flow (filters GP partners out, focus on standard Women)
    val filteredPartners: StateFlow<List<Partner>> = combine(
        allPartners,
        allEncontros,
        snapshotFlow { partnerSearchQuery },
        snapshotFlow { activePartnerFilter },
        snapshotFlow { partnerSortOption }
    ) { partners, encuentros, query, filter, sort ->
        var list = partners.filter { !it.isGP }
        
        val encountersByPartner = encuentros.groupBy { it.partnerId }
        
        if (query.isNotEmpty()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.location.contains(query, ignoreCase = true) ||
                        it.notes.contains(query, ignoreCase = true) ||
                        it.origin.contains(query, ignoreCase = true)
            }
        }
        
        when (filter) {
            PartnerFilter.TODOS -> {}
            PartnerFilter.ATIVAS -> {
                list = list.filter { it.status.equals("Ativa", ignoreCase = true) }
            }
            PartnerFilter.SEM_SEXO -> {
                list = list.filter { partner ->
                    val partnerEncs = encountersByPartner[partner.id] ?: emptyList()
                    val hasSex = partnerEncs.any { it.hadSex && (it.typeVaginal || it.typeAnal || it.typeOral) }
                    !hasSex
                }
            }
            PartnerFilter.TRANSADAS -> {
                list = list.filter { partner ->
                    val partnerEncs = encountersByPartner[partner.id] ?: emptyList()
                    val hasSex = partnerEncs.any { it.hadSex && (it.typeVaginal || it.typeAnal || it.typeOral) }
                    hasSex
                }
            }
            PartnerFilter.INATIVAS -> {
                list = list.filter { it.status.equals("Inativa", ignoreCase = true) }
            }
            PartnerFilter.SEM_FILHOS -> {
                list = list.filter { it.children == 0 }
            }
            PartnerFilter.FAZ_ANAL -> {
                list = list.filter { partner ->
                    val partnerEncs = encountersByPartner[partner.id] ?: emptyList()
                    val hasAnal = partnerEncs.any { it.typeAnal }
                    hasAnal
                }
            }
        }

        // Apply selected sorting criteria
        list = when (sort) {
            PartnerSortOption.ABSTINENCIA -> {
                list.sortedWith(compareBy<Partner> { partner ->
                    val partnerEncs = encountersByPartner[partner.id] ?: emptyList()
                    val sexEncs = partnerEncs.filter { it.hadSex && (it.typeVaginal || it.typeAnal || it.typeOral) }
                    val lastSexDate = sexEncs.maxByOrNull { it.date }?.date
                    if (lastSexDate != null) {
                        calculateDaysBetween(lastSexDate, getTodayDateString())
                    } else {
                        Int.MAX_VALUE // Send those without registered sex to the end
                    }
                }.thenBy { it.name })
            }
            PartnerSortOption.RANKING -> {
                list.sortedWith(compareByDescending<Partner> { partner ->
                    val partnerEncontros = encountersByPartner[partner.id] ?: emptyList()
                    com.example.data.WomanCalculator.calculateScoreW(partner, partnerEncontros)
                }.thenBy { it.name })
            }
            PartnerSortOption.NOME -> {
                list.sortedBy { it.name.lowercase() }
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // GPs list flow
    val filteredGPs: StateFlow<List<Partner>> = combine(
        allPartners,
        snapshotFlow { gpSearchQuery }
    ) { partners, query ->
        var list = partners.filter { it.isGP }
        if (query.isNotEmpty()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.location.contains(query, ignoreCase = true) || // location contains establishment
                        it.notes.contains(query, ignoreCase = true) ||
                        it.origin.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations & Metrics (Computed Dynamically from SQLite Room inputs based on abstinenceFilter)
    val daysOfAbstinence: StateFlow<Int> = combine(
        allPartners,
        allEncontros,
        snapshotFlow { abstinenceFilter },
        snapshotFlow { mostrarGP }
    ) { partners, encounters, filter, gpVisible ->
        val sexEncontros = encounters.filter { enc ->
            val p = partners.find { it.id == enc.partnerId }
            if (p == null) false else {
                val matchesFilter = when (filter) {
                    "Sem GP" -> !p.isGP
                    "Só GP" -> p.isGP
                    else -> if (!gpVisible) !p.isGP else true
                }
                matchesFilter && enc.hadSex && (enc.typeVaginal || enc.typeAnal || enc.typeOral)
            }
        }
        if (sexEncontros.isEmpty()) {
            -1 // Represents Infinity / No Record Yet
        } else {
            val sorted = sexEncontros.sortedByDescending { it.date }
            val latest = sorted.first()
            calculateDaysBetween(latest.date, getTodayDateString())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    val latestSexPartnerName: StateFlow<String> = combine(
        allPartners,
        allEncontros,
        snapshotFlow { abstinenceFilter },
        snapshotFlow { mostrarGP }
    ) { partners, encounters, filter, gpVisible ->
        val sexEncontros = encounters.filter { enc ->
            val p = partners.find { it.id == enc.partnerId }
            if (p == null) false else {
                val matchesFilter = when (filter) {
                    "Sem GP" -> !p.isGP
                    "Só GP" -> p.isGP
                    else -> if (!gpVisible) !p.isGP else true
                }
                matchesFilter && enc.hadSex && (enc.typeVaginal || enc.typeAnal || enc.typeOral)
            }
        }
        if (sexEncontros.isEmpty()) {
            "Sem registro"
        } else {
            val latest = sexEncontros.sortedByDescending { it.date }.first()
            latest.partnerName
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Sem registro")

    val latestEncounterRelTimeString: StateFlow<String> = combine(
        allPartners,
        allEncontros,
        snapshotFlow { abstinenceFilter },
        snapshotFlow { mostrarGP }
    ) { partners, encounters, filter, gpVisible ->
        val sexEncontros = encounters.filter { enc ->
            val p = partners.find { it.id == enc.partnerId }
            if (p == null) false else {
                val matchesFilter = when (filter) {
                    "Sem GP" -> !p.isGP
                    "Só GP" -> p.isGP
                    else -> if (!gpVisible) !p.isGP else true
                }
                matchesFilter && enc.hadSex && (enc.typeVaginal || enc.typeAnal || enc.typeOral)
            }
        }
        if (sexEncontros.isEmpty()) {
            "Sem data"
        } else {
            val latest = sexEncontros.sortedByDescending { it.date }.first()
            val days = calculateDaysBetween(latest.date, getTodayDateString())
            when (days) {
                0 -> "Hoje"
                1 -> "Ontem"
                else -> "Há $days dias"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Sem data")

    val latestEncounterCity: StateFlow<String> = combine(
        allPartners,
        allEncontros,
        snapshotFlow { abstinenceFilter },
        snapshotFlow { mostrarGP }
    ) { partners, encounters, filter, gpVisible ->
        val sexEncontros = encounters.filter { enc ->
            val p = partners.find { it.id == enc.partnerId }
            if (p == null) false else {
                val matchesFilter = when (filter) {
                    "Sem GP" -> !p.isGP
                    "Só GP" -> p.isGP
                    else -> if (!gpVisible) !p.isGP else true
                }
                matchesFilter && enc.hadSex && (enc.typeVaginal || enc.typeAnal || enc.typeOral)
            }
        }
        if (sexEncontros.isEmpty()) {
            "Sem registro"
        } else {
            val latest = sexEncontros.sortedByDescending { it.date }.first()
            val partner = partners.find { it.id == latest.partnerId }
            if (partner != null) {
                partner.getCity()
            } else {
                "Sem registro"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Sem registro")

    // General Aggregate Stats - normal women
    val normalSexCount: StateFlow<Int> = combine(allPartners, allEncontros) { partners, encounters ->
        encounters.filter { enc -> 
            val p = partners.find { it.id == enc.partnerId }
            p != null && !p.isGP && enc.hadSex && (enc.typeVaginal || enc.typeAnal || enc.typeOral)
        }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val normalAnalCount: StateFlow<Int> = combine(allPartners, allEncontros) { partners, encounters ->
        encounters.filter { enc -> 
            val p = partners.find { it.id == enc.partnerId }
            p != null && !p.isGP && enc.typeAnal
        }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val normalCreampieCount: StateFlow<Int> = combine(allPartners, allEncontros) { partners, encounters ->
        encounters.filter { enc -> 
            val p = partners.find { it.id == enc.partnerId }
            p != null && !p.isGP && enc.typeCreampie
        }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val uniqueWomenWithSex: StateFlow<Int> = combine(allPartners, allEncontros) { partners, encounters ->
        val sexPartnerIds = encounters.filter { it.hadSex && (it.typeVaginal || it.typeAnal || it.typeOral) }.map { it.partnerId }.toSet()
        partners.count { !it.isGP && sexPartnerIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Current Year Stats
    val currentYearString = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())

    val normalSexThisYear: StateFlow<Int> = combine(allPartners, allEncontros) { partners, encounters ->
        encounters.filter { enc -> 
            val p = partners.find { it.id == enc.partnerId }
            p != null && !p.isGP && enc.hadSex && (enc.typeVaginal || enc.typeAnal || enc.typeOral) && enc.date.startsWith(currentYearString)
        }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val normalAnalThisYear: StateFlow<Int> = combine(allPartners, allEncontros) { partners, encounters ->
        encounters.filter { enc -> 
            val p = partners.find { it.id == enc.partnerId }
            p != null && !p.isGP && enc.typeAnal && enc.date.startsWith(currentYearString)
        }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val partnersThisYear: StateFlow<Int> = combine(allPartners, allEncontros) { partners, encounters ->
        val partnerIdsThisYear = encounters.filter { it.date.startsWith(currentYearString) }.map { it.partnerId }.toSet()
        partners.count { !it.isGP && partnerIdsThisYear.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // GP Aggregate Stats
    val gpEncountersCount: StateFlow<Int> = combine(allPartners, allEncontros) { partners, encounters ->
        encounters.filter { enc ->
            val p = partners.find { it.id == enc.partnerId }
            p != null && p.isGP
        }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val gpAnalCount: StateFlow<Int> = combine(allPartners, allEncontros) { partners, encounters ->
        encounters.filter { enc ->
            val p = partners.find { it.id == enc.partnerId }
            p != null && p.isGP
        }.sumOf { maxOf(it.countAnal, if (it.typeAnal) 1 else 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val gpOralCount: StateFlow<Int> = combine(allPartners, allEncontros) { partners, encounters ->
        encounters.filter { enc ->
            val p = partners.find { it.id == enc.partnerId }
            p != null && p.isGP
        }.sumOf { maxOf(it.countOral, if (it.typeOral) 1 else 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val gpSpentTotal: StateFlow<Double> = combine(allPartners, allEncontros) { partners, encounters ->
        encounters.filter { enc ->
            val p = partners.find { it.id == enc.partnerId }
            p != null && p.isGP
        }.sumOf { it.motelCost } // motelCost stores price in entry for GPs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val gpEstoquecount: StateFlow<Int> = allPartners.map { list -> list.count { it.isGP } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Efficiency calculations
    val conversionRate: StateFlow<Double> = combine(allPartners, allEncontros) { partners, encounters ->
        val standardWomen = partners.filter { !it.isGP }
        if (standardWomen.isEmpty()) return@combine 0.0
        val sexPartnerIds = encounters.filter { it.hadSex && (it.typeVaginal || it.typeAnal || it.typeOral) }.map { it.partnerId }.toSet()
        val transadas = standardWomen.count { sexPartnerIds.contains(it.id) }
        (transadas.toDouble() / standardWomen.size.toDouble()) * 100.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val efficiencyMeetingsToSex: StateFlow<Double> = combine(allPartners, allEncontros) { partners, encounters ->
        val standardEncounters = encounters.filter { enc ->
            val p = partners.find { it.id == enc.partnerId }
            p != null && !p.isGP
        }
        if (standardEncounters.isEmpty()) return@combine 0.0
        val withSex = standardEncounters.count { it.hadSex && (it.typeVaginal || it.typeAnal || it.typeOral) }
        (withSex.toDouble() / standardEncounters.size.toDouble()) * 100.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageMeetingsUntilSex: StateFlow<Double> = combine(allPartners, allEncontros) { partners, encounters ->
        val standardWomen = partners.filter { !it.isGP }
        var totalMeetingsBeforeFirstSex = 0
        var convertedCount = 0

        val encountersByPartner = encounters.groupBy { it.partnerId }

        for (woman in standardWomen) {
            val partnerEncs = encountersByPartner[woman.id] ?: emptyList()
            val sortedEnc = partnerEncs.sortedBy { it.date }
            if (sortedEnc.isEmpty()) continue
            
            // Find index of first sex encounter
            val firstSexIdx = sortedEnc.indexOfFirst { it.hadSex && (it.typeVaginal || it.typeAnal || it.typeOral) }
            if (firstSexIdx != -1) {
                totalMeetingsBeforeFirstSex += (firstSexIdx + 1)
                convertedCount++
            }
        }
        if (convertedCount == 0) return@combine 0.0
        totalMeetingsBeforeFirstSex.toDouble() / convertedCount.toDouble()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Motel & GP Costs combined
    val totalMotelCost: StateFlow<Double> = combine(allPartners, allEncontros) { partners, encounters ->
        encounters.filter { enc ->
            val p = partners.find { it.id == enc.partnerId }
            p != null && !p.isGP // only count standard women motel cost
        }.sumOf { it.motelCost }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Check if partner ID is the globally most recent sex encounter
    fun checkIsMostRecentSexGlobal(partnerId: Int, encounters: List<Encontro>): Boolean {
        val sexEnc = encounters.filter { it.hadSex }
        if (sexEnc.isEmpty()) return false
        val latest = sexEnc.maxWithOrNull(compareBy<Encontro> { it.date }.thenBy { it.id })
        return latest?.partnerId == partnerId
    }

    // CRUD database commands - fully compatible and extended
    fun addPartnerAndGetId(partner: Partner, onResult: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertPartner(partner)
            onResult(id)
        }
    }

    fun addPartner(
        name: String,
        location: String,
        origin: String,
        age: Int,
        notes: String,
        isFavorite: Boolean,
        photoUrl: String,
        rating: Double,
        children: Int = 0,
        myKids: Int = 0,
        objective: String = "Casual",
        phone: String = "",
        address: String = "",
        availability: String = "Solteira",
        status: String = "Ativa",
        negatives: String = "",
        isGP: Boolean = false,
        gpPrice: Double = 0.0
    ) {
        viewModelScope.launch {
            repository.insertPartner(
                Partner(
                    name = name,
                    location = location,
                    origin = origin,
                    age = age,
                    notes = notes,
                    isFavorite = isFavorite,
                    photoUrl = photoUrl,
                    rating = rating,
                    firstDate = if (isGP) "" else getTodayDateString(),
                    children = children,
                    myKids = myKids,
                    objective = objective,
                    phone = phone,
                    address = address,
                    availability = availability,
                    status = status,
                    negatives = negatives,
                    isGP = isGP,
                    gpPrice = gpPrice,
                    createdAt = System.currentTimeMillis()
                )
            )
            Toast.makeText(getApplication(), if (isGP) "GP cadastrada com sucesso!" else "Parceira salva com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    fun addEncontro(
        partnerId: Int,
        date: String,
        time: String,
        motelCost: Double,
        typeOral: Boolean,
        typeAnal: Boolean,
        typeVaginal: Boolean,
        typeCreampie: Boolean,
        notes: String,
        hadSex: Boolean = true,
        isPregnancy: Boolean = false,
        rating: Double = 5.0,
        typeFacial: Boolean = false,
        typeSquirt: Boolean = false,
        typeDeepthroat: Boolean = false,
        countOral: Int = 0,
        countAnal: Int = 0,
        countVaginal: Int = 0,
        countCreampie: Int = 0,
        countFacial: Int = 0,
        countSquirt: Int = 0,
        countDeepthroat: Int = 0,
        countAnalCreampie: Int = 0,
        isFirstEncontroSex: Boolean = false,
        isVirginityLost: Boolean = false,
        typeAnalCreampie: Boolean = false,
        isPregnancyMarked: Boolean = false,
        gpSexoVaginal: Boolean = false,
        gpOralSemCamisinha: Boolean = false,
        gpSexoAnal: Boolean = false,
        gpBeijoNaBoca: Boolean = false,
        gp69ComCamisinha: Boolean = false,
        gpMassagemErotica: Boolean = false,
        gpDominacaoBdsm: Boolean = false,
        gpBeijoGrego: Boolean = false,
        gpInversaoFetiche: Boolean = false,
        gpGargantaProfunda: Boolean = false,
        gpFetichePe: Boolean = false,
        gpFioTerra: Boolean = false,
        gpGastoValor: Double = 0.0
    ) {
        viewModelScope.launch {
            val partner = repository.getPartnerById(partnerId)
            val partnerName = partner?.name ?: "Desconhecida"
            repository.insertEncontro(
                Encontro(
                    partnerId = partnerId,
                    partnerName = partnerName,
                    date = date,
                    time = time,
                    motelCost = motelCost,
                    typeOral = typeOral,
                    typeAnal = typeAnal,
                    typeVaginal = typeVaginal,
                    typeCreampie = typeCreampie,
                    typeFacial = typeFacial,
                    typeSquirt = typeSquirt,
                    typeDeepthroat = typeDeepthroat,
                    notes = notes,
                    hadSex = hadSex,
                    isPregnancy = isPregnancy,
                    rating = rating,
                    countOral = countOral,
                    countAnal = countAnal,
                    countVaginal = countVaginal,
                    countCreampie = countCreampie,
                    countFacial = countFacial,
                    countSquirt = countSquirt,
                    countDeepthroat = countDeepthroat,
                    countAnalCreampie = countAnalCreampie,
                    isFirstEncontroSex = isFirstEncontroSex,
                    isVirginityLost = isVirginityLost,
                    typeAnalCreampie = typeAnalCreampie,
                    isPregnancyMarked = isPregnancyMarked,
                    gpSexoVaginal = gpSexoVaginal,
                    gpOralSemCamisinha = gpOralSemCamisinha,
                    gpSexoAnal = gpSexoAnal,
                    gpBeijoNaBoca = gpBeijoNaBoca,
                    gp69ComCamisinha = gp69ComCamisinha,
                    gpMassagemErotica = gpMassagemErotica,
                    gpDominacaoBdsm = gpDominacaoBdsm,
                    gpBeijoGrego = gpBeijoGrego,
                    gpInversaoFetiche = gpInversaoFetiche,
                    gpGargantaProfunda = gpGargantaProfunda,
                    gpFetichePe = gpFetichePe,
                    gpFioTerra = gpFioTerra,
                    gpGastoValor = gpGastoValor
                )
            )
            Toast.makeText(getApplication(), "Encontro registrado com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deletePartner(partner: Partner) {
        viewModelScope.launch {
            repository.deletePartner(partner)
            Toast.makeText(getApplication(), "Removido.", Toast.LENGTH_SHORT).show()
        }
    }

    fun updatePartner(partner: Partner) {
        viewModelScope.launch {
            repository.updatePartner(partner)
            Toast.makeText(getApplication(), "Perfil atualizado!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteEncontro(encontro: Encontro) {
        viewModelScope.launch {
            repository.deleteEncontro(encontro)
            Toast.makeText(getApplication(), "Encontro removido.", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateEncontro(encontro: Encontro) {
        viewModelScope.launch {
            repository.updateEncontro(encontro)
            Toast.makeText(getApplication(), "Encontro atualizado!", Toast.LENGTH_SHORT).show()
        }
    }

    // JSON Backup & Restore - Spec 21
    fun getBackupJsonString(): String {
        try {
            val root = JSONObject()
            val partnersArray = JSONArray()
            for (p in allPartners.value) {
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("location", p.location)
                obj.put("origin", p.origin)
                obj.put("age", p.age)
                obj.put("affinity", p.affinity)
                obj.put("photoUrl", p.photoUrl)
                obj.put("isFavorite", p.isFavorite)
                obj.put("notes", p.notes)
                obj.put("firstDate", p.firstDate)
                obj.put("rating", p.rating)
                
                // Extra specs
                obj.put("children", p.children)
                obj.put("myKids", p.myKids)
                obj.put("objective", p.objective)
                obj.put("phone", p.phone)
                obj.put("address", p.address)
                obj.put("availability", p.availability)
                obj.put("status", p.status)
                obj.put("negatives", p.negatives)
                obj.put("isGP", p.isGP)
                obj.put("gpPrice", p.gpPrice)
                obj.put("createdAt", p.createdAt)
                partnersArray.put(obj)
            }
            root.put("partners", partnersArray)

            val encontrosArray = JSONArray()
            for (e in allEncontros.value) {
                val obj = JSONObject()
                obj.put("id", e.id)
                obj.put("partnerId", e.partnerId)
                obj.put("partnerName", e.partnerName)
                obj.put("date", e.date)
                obj.put("time", e.time)
                obj.put("motelCost", e.motelCost)
                obj.put("typeOral", e.typeOral)
                obj.put("typeAnal", e.typeAnal)
                obj.put("typeVaginal", e.typeVaginal)
                obj.put("typeCreampie", e.typeCreampie)
                obj.put("typeFacial", e.typeFacial)
                obj.put("typeSquirt", e.typeSquirt)
                obj.put("typeDeepthroat", e.typeDeepthroat)
                obj.put("typeFirstEncounter", e.typeFirstEncounter)
                obj.put("typeVirginity", e.typeVirginity)
                obj.put("notes", e.notes)
                obj.put("hadSex", e.hadSex)
                obj.put("isPregnancy", e.isPregnancy)
                obj.put("rating", e.rating)
                encontrosArray.put(obj)
            }
            root.put("encontros", encontrosArray)
            return root.toString(4)
        } catch (e: Exception) {
            e.printStackTrace()
            return "{}"
        }
    }

    fun exportBackup() {
        try {
            val jsonStr = getBackupJsonString()
            // Save to internal app files cache so user has a local copy
            val file = File(getApplication<Application>().cacheDir, "agenda_h_backup.json")
            file.writeText(jsonStr)
            Toast.makeText(getApplication(), "Backup temporário exportado em (${file.name})!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(getApplication(), "Erro ao criar backup temporário: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportBackupToUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonStr = getBackupJsonString()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonStr.toByteArray())
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup salvo com sucesso no seu celular!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro ao salvar backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    suspend fun parseAndImportJsonString(jsonStr: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                var cleanStr = jsonStr.trim()
                if (cleanStr.isEmpty()) return@withContext false

                // Strip Markdown code block styling if any
                if (cleanStr.contains("```json")) {
                    val start = cleanStr.indexOf("```json") + 7
                    val end = cleanStr.indexOf("```", start)
                    if (end > start) {
                        cleanStr = cleanStr.substring(start, end).trim()
                    }
                } else if (cleanStr.contains("```")) {
                    val start = cleanStr.indexOf("```") + 3
                    val end = cleanStr.indexOf("```", start)
                    if (end > start) {
                        cleanStr = cleanStr.substring(start, end).trim()
                    }
                }

                // If there is preamble or text wrapping, isolate the JSON structure
                if (!cleanStr.startsWith("{") && !cleanStr.startsWith("[")) {
                    val firstCurly = cleanStr.indexOf("{")
                    val firstSquare = cleanStr.indexOf("[")
                    if (firstCurly != -1 && (firstSquare == -1 || firstCurly < firstSquare)) {
                        val lastCurly = cleanStr.lastIndexOf("}")
                        if (lastCurly > firstCurly) {
                            cleanStr = cleanStr.substring(firstCurly, lastCurly + 1)
                        }
                    } else if (firstSquare != -1) {
                        val lastSquare = cleanStr.lastIndexOf("]")
                        if (lastSquare > firstSquare) {
                            cleanStr = cleanStr.substring(firstSquare, lastSquare + 1)
                        }
                    }
                }

                // Do NOT wipe existing data! Fetch existing records to preserve IDs and avoid corruption.
                val existingPartners = repository.getAllPartnersList()
                val existingEncontros = repository.getAllEncontrosList()

                val existingPartnerIds = existingPartners.map { it.id }.toMutableSet()
                val existingPartnerByName = existingPartners.associateBy { it.name.trim().lowercase() }.toMutableMap()
                var maxPartnerId = existingPartners.maxOfOrNull { it.id } ?: 0

                val existingEncounterIds = existingEncontros.map { it.id }.toMutableSet()
                var maxEncounterId = existingEncontros.maxOfOrNull { it.id } ?: 0

                val rootArray: JSONArray? = if (cleanStr.startsWith("[")) {
                    JSONArray(cleanStr)
                } else {
                    null
                }

                val rootObject: JSONObject? = if (cleanStr.startsWith("{")) {
                    JSONObject(cleanStr)
                } else {
                    null
                }

                val idMap = mutableMapOf<String, Int>()

                suspend fun importEncounterJson(encObj: JSONObject, defaultPartnerId: Int, partnerNameDef: String, partnerIsGP: Boolean) {
                    val originalPartnerIdStr = encObj.optString("partnerId", encObj.optString("womanId", encObj.optString("mulherId", "")))
                    val originalPartnerIdInt = encObj.optInt("partnerId", encObj.optInt("womanId", encObj.optInt("mulherId", defaultPartnerId)))
                    val partnerName = encObj.optString("partnerName", encObj.optString("nome", encObj.optString("partner", partnerNameDef)))
                    val partnerNameKey = partnerName.trim().lowercase()

                    val mappedPartnerId = if (originalPartnerIdStr.isNotEmpty() && idMap.containsKey(originalPartnerIdStr)) {
                        idMap[originalPartnerIdStr] ?: defaultPartnerId
                    } else if (idMap.containsKey(originalPartnerIdInt.toString())) {
                        idMap[originalPartnerIdInt.toString()] ?: defaultPartnerId
                    } else if (partnerNameKey.isNotEmpty() && idMap.containsKey(partnerNameKey)) {
                        idMap[partnerNameKey] ?: defaultPartnerId
                    } else if (partnerNameKey.isNotEmpty() && existingPartnerByName.containsKey(partnerNameKey)) {
                        existingPartnerByName[partnerNameKey]?.id ?: defaultPartnerId
                    } else if (originalPartnerIdInt != 0 && existingPartnerIds.contains(originalPartnerIdInt)) {
                        originalPartnerIdInt
                    } else {
                        defaultPartnerId
                    }
                    if (mappedPartnerId == 0) return

                    val date = encObj.optString("date", encObj.optString("data", ""))
                    if (date.isBlank()) return

                    val time = encObj.optString("time", encObj.optString("hora", encObj.optString("horario", "")))
                    val motelCost = encObj.optDouble("motelCost", encObj.optDouble("custo", encObj.optDouble("valor", encObj.optDouble("gasto", encObj.optDouble("preco", 0.0)))))
                    
                    // Parse inner fetishes array structure
                    val fetishesArray = encObj.optJSONArray("fetishes") ?: encObj.optJSONArray("fetiches")
                    var parsedVaginal = false
                    var parsedAnal = false
                    var parsedCreampie = false
                    var parsedAnalCreampie = false
                    var parsedOral = false
                    var parsedFacial = false
                    var parsedSquirt = false
                    var parsedDeepthroat = false
                    var parsedFirstEncounter = false
                    var parsedVirginity = false
                    var parsedBeijoGrego = false
                    var parsedInversao = false
                    var parsedFetichePe = false
                    var parsedFioTerra = false

                    var fVaginalCount = 0
                    var fAnalCount = 0
                    var fCreampieCount = 0
                    var fAnalCreampieCount = 0
                    var fOralCount = 0
                    var fFacialCount = 0
                    var fSquirtCount = 0
                    var fDeepthroatCount = 0

                    if (fetishesArray != null) {
                        for (k in 0 until fetishesArray.length()) {
                            val fetObj = fetishesArray.getJSONObject(k)
                            val fType = fetObj.optString("type", "").lowercase()
                            val fCount = fetObj.optInt("count", 1)
                            when (fType) {
                                "vaginal" -> {
                                    parsedVaginal = true
                                    fVaginalCount = fCount
                                }
                                "anal" -> {
                                    parsedAnal = true
                                    fAnalCount = fCount
                                }
                                "creampie", "vaginal_creampie" -> {
                                    parsedCreampie = true
                                    fCreampieCount = fCount
                                }
                                "analcreampie", "anal_creampie" -> {
                                    parsedAnalCreampie = true
                                    fAnalCreampieCount = fCount
                                }
                                "oral" -> {
                                    parsedOral = true
                                    fOralCount = fCount
                                }
                                "facial" -> {
                                    parsedFacial = true
                                    fFacialCount = fCount
                                }
                                "squirt" -> {
                                    parsedSquirt = true
                                    fSquirtCount = fCount
                                }
                                "deepthroat", "garganta_profunda" -> {
                                    parsedDeepthroat = true
                                    fDeepthroatCount = fCount
                                }
                                "firstencounter", "first_encounter" -> {
                                    parsedFirstEncounter = true
                                }
                                "virginity" -> {
                                    parsedVirginity = true
                                }
                                "beijogrego", "gpbeijogrego", "beijo_grego" -> {
                                    parsedBeijoGrego = true
                                }
                                "inversao", "inversaofetiche", "gpinversaofetiche" -> {
                                    parsedInversao = true
                                }
                                "fetichepe", "gpfetichepe", "pe", "pé" -> {
                                    parsedFetichePe = true
                                }
                                "fioterra", "gpfioterra", "fio_terra" -> {
                                    parsedFioTerra = true
                                }
                            }
                        }
                    }

                    val typeOral = encObj.optBoolean("typeOral", encObj.optBoolean("oral", parsedOral)) || parsedOral
                    val typeAnal = encObj.optBoolean("typeAnal", encObj.optBoolean("anal", parsedAnal)) || parsedAnal
                    val typeVaginal = encObj.optBoolean("typeVaginal", encObj.optBoolean("vaginal", parsedVaginal)) || parsedVaginal
                    val typeCreampie = encObj.optBoolean("typeCreampie", encObj.optBoolean("creampie", parsedCreampie)) || parsedCreampie
                    val typeFacial = encObj.optBoolean("typeFacial", encObj.optBoolean("facial", parsedFacial)) || parsedFacial
                    val typeSquirt = encObj.optBoolean("typeSquirt", encObj.optBoolean("squirt", parsedSquirt)) || parsedSquirt
                    val typeDeepthroat = encObj.optBoolean("typeDeepthroat", encObj.optBoolean("deepthroat", parsedDeepthroat)) || parsedDeepthroat
                    val typeFirstEncounter = encObj.optBoolean("typeFirstEncounter", encObj.optBoolean("primeiro", parsedFirstEncounter)) || parsedFirstEncounter
                    val typeVirginity = encObj.optBoolean("typeVirginity", encObj.optBoolean("virgem", parsedVirginity)) || parsedVirginity
                    
                    val notes = encObj.optString("notes", encObj.optString("notas", encObj.optString("obs", encObj.optString("observacao", ""))))
                    val hadSex = encObj.optBoolean("hadSex", encObj.optBoolean("sexo", true))
                    val isPregnancy = encObj.optBoolean("isPregnancy", encObj.optBoolean("gravidez", encObj.optBoolean("suspeita", false)))
                    val rating = encObj.optDouble("rating", encObj.optDouble("nota", encObj.optDouble("avaliacao", 5.0)))

                    val countOral = encObj.optInt("countOral", if (fOralCount > 0) fOralCount else (if (typeOral) 1 else 0))
                    val countAnal = encObj.optInt("countAnal", if (fAnalCount > 0) fAnalCount else (if (typeAnal) 1 else 0))
                    val countVaginal = encObj.optInt("countVaginal", if (fVaginalCount > 0) fVaginalCount else (if (typeVaginal) 1 else 0))
                    val countCreampie = encObj.optInt("countCreampie", if (fCreampieCount > 0) fCreampieCount else (if (typeCreampie) 1 else 0))
                    val countFacial = encObj.optInt("countFacial", if (fFacialCount > 0) fFacialCount else (if (typeFacial) 1 else 0))
                    val countSquirt = encObj.optInt("countSquirt", if (fSquirtCount > 0) fSquirtCount else (if (typeSquirt) 1 else 0))
                    val countDeepthroat = encObj.optInt("countDeepthroat", if (fDeepthroatCount > 0) fDeepthroatCount else (if (typeDeepthroat) 1 else 0))
                    val countAnalCreampie = encObj.optInt("countAnalCreampie", if (fAnalCreampieCount > 0) fAnalCreampieCount else (if (encObj.optBoolean("typeAnalCreampie", parsedAnalCreampie)) 1 else 0))

                    // Aba Mulheres fields mapping
                    val isFirstEncontroSex = encObj.optBoolean("isFirstEncontroSex", typeFirstEncounter)
                    val isVirginityLost = encObj.optBoolean("isVirginityLost", typeVirginity)
                    val typeAnalCreampie = encObj.optBoolean("typeAnalCreampie", typeAnal && typeCreampie) || parsedAnalCreampie
                    val isPregnancyMarked = encObj.optBoolean("isPregnancyMarked", isPregnancy)

                    // Aba GP fields mapping
                    val gpSexoVaginal = encObj.optBoolean("gpSexoVaginal", if (partnerIsGP) typeVaginal else false) || (partnerIsGP && parsedVaginal)
                    val gpOralSemCamisinha = encObj.optBoolean("gpOralSemCamisinha", if (partnerIsGP) typeOral else false) || (partnerIsGP && parsedOral)
                    val gpSexoAnal = encObj.optBoolean("gpSexoAnal", if (partnerIsGP) typeAnal else false) || (partnerIsGP && parsedAnal)
                    val gpBeijoNaBoca = encObj.optBoolean("gpBeijoNaBoca", if (partnerIsGP) typeCreampie else false) || (partnerIsGP && parsedCreampie)
                    val gp69ComCamisinha = encObj.optBoolean("gp69ComCamisinha", if (partnerIsGP) typeFacial else false) || (partnerIsGP && parsedFacial)
                    val gpMassagemErotica = encObj.optBoolean("gpMassagemErotica", if (partnerIsGP) typeSquirt else false) || (partnerIsGP && parsedSquirt)
                    val gpDominacaoBdsm = encObj.optBoolean("gpDominacaoBdsm", if (partnerIsGP) typeDeepthroat else false) || (partnerIsGP && parsedDeepthroat)
                    
                    val gpBeijoGrego = encObj.optBoolean("gpBeijoGrego", parsedBeijoGrego)
                    val gpInversaoFetiche = encObj.optBoolean("gpInversaoFetiche", parsedInversao)
                    val gpGargantaProfunda = encObj.optBoolean("gpGargantaProfunda", if (partnerIsGP) typeDeepthroat else false) || (partnerIsGP && parsedDeepthroat)
                    val gpFetichePe = encObj.optBoolean("gpFetichePe", parsedFetichePe)
                    val gpFioTerra = encObj.optBoolean("gpFioTerra", parsedFioTerra)
                    val gpGastoValor = encObj.optDouble("gpGastoValor", if (partnerIsGP) motelCost else 0.0)

                    val originalEncId = encObj.optInt("id", 0)
                    val targetEncId = if (originalEncId > 0 && originalEncId !in existingEncounterIds) {
                        existingEncounterIds.add(originalEncId)
                        maxEncounterId = maxOf(maxEncounterId, originalEncId)
                        originalEncId
                    } else {
                        val isDuplicate = existingEncontros.any {
                            it.partnerId == mappedPartnerId && it.date == date && (time.isEmpty() || it.time == time)
                        }
                        if (isDuplicate) return
                        maxEncounterId++
                        existingEncounterIds.add(maxEncounterId)
                        maxEncounterId
                    }

                    val encounter = Encontro(
                        id = targetEncId,
                        partnerId = mappedPartnerId,
                        partnerName = partnerName,
                        date = date,
                        time = time,
                        motelCost = motelCost,
                        typeOral = typeOral,
                        typeAnal = typeAnal,
                        typeVaginal = typeVaginal,
                        typeCreampie = typeCreampie,
                        typeFacial = typeFacial,
                        typeSquirt = typeSquirt,
                        typeDeepthroat = typeDeepthroat,
                        typeFirstEncounter = typeFirstEncounter,
                        typeVirginity = typeVirginity,
                        notes = notes,
                        hadSex = hadSex,
                        isPregnancy = isPregnancy,
                        rating = rating,
                        countOral = countOral,
                        countAnal = countAnal,
                        countVaginal = countVaginal,
                        countCreampie = countCreampie,
                        countFacial = countFacial,
                        countSquirt = countSquirt,
                        countDeepthroat = countDeepthroat,
                        countAnalCreampie = countAnalCreampie,
                        isFirstEncontroSex = isFirstEncontroSex,
                        isVirginityLost = isVirginityLost,
                        typeAnalCreampie = typeAnalCreampie,
                        isPregnancyMarked = isPregnancyMarked,
                        gpSexoVaginal = gpSexoVaginal,
                        gpOralSemCamisinha = gpOralSemCamisinha,
                        gpSexoAnal = gpSexoAnal,
                        gpBeijoNaBoca = gpBeijoNaBoca,
                        gp69ComCamisinha = gp69ComCamisinha,
                        gpMassagemErotica = gpMassagemErotica,
                        gpDominacaoBdsm = gpDominacaoBdsm,
                        gpBeijoGrego = gpBeijoGrego,
                        gpInversaoFetiche = gpInversaoFetiche,
                        gpGargantaProfunda = gpGargantaProfunda,
                        gpFetichePe = gpFetichePe,
                        gpFioTerra = gpFioTerra,
                        gpGastoValor = gpGastoValor
                    )
                    repository.insertEncontro(encounter)
                    existingEncounterIds.add(targetEncId)
                }

                suspend fun importPartnerJson(obj: JSONObject, forceGP: Boolean = false) {
                    val originalIdStr = obj.optString("id", "")
                    
                    val name = obj.optString("name", obj.optString("nome", ""))
                    if (name.isBlank()) return
                    
                    val rawLocation = obj.optString("location", obj.optString("local", obj.optString("bairro", obj.optString("cidade", "Mesma cidade / <5km"))))
                    val location = when (rawLocation.trim().lowercase()) {
                        "nearby" -> "Mesma cidade / <5km"
                        "region" -> "Município próximo (5-20km)"
                        "samestate" -> "Mesmo estado (>20km)"
                        "otherstate" -> "Outro estado"
                        "abroad" -> "Exterior"
                        else -> {
                            if (listOf("Mesma cidade / <5km", "Município próximo (5-20km)", "Mesmo estado (>20km)", "Outro estado", "Exterior").any { it.equals(rawLocation, ignoreCase = true) }) {
                                rawLocation
                            } else {
                                "Mesma cidade / <5km"
                            }
                        }
                    }

                    val origin = obj.optString("origin", obj.optString("origem", "Outros"))
                    val age = obj.optInt("age", obj.optInt("idade", 0))
                    val affinity = obj.optInt("affinity", obj.optInt("afinidade", 50))
                    val photoUrl = obj.optString("photoUrl", obj.optString("foto", ""))
                    val isFavorite = obj.optBoolean("isFavorite", obj.optBoolean("favorito", false))
                    val notes = obj.optString("notes", obj.optString("notas", obj.optString("observacoes", obj.optString("obs", ""))))
                    val firstDate = obj.optString("firstDate", obj.optString("primeiroEncontro", ""))
                    val rating = obj.optDouble("rating", obj.optDouble("nota", obj.optDouble("estrela", obj.optDouble("estrelas", 5.0))))
                    
                    val children = obj.optInt("children", obj.optInt("filhos", 0))
                    val myKids = obj.optInt("myKids", obj.optInt("meusFilhos", 0))
                    
                    val rawObjective = obj.optString("objective", obj.optString("objetivo", "Casual"))
                    val objective = when (rawObjective.trim().lowercase()) {
                        "serious" -> "Relacionamento sério"
                        "casual" -> "Casual"
                        "maybe" -> "Ver no que dá"
                        "friendship", "chat" -> "Amizade / só conversar"
                        "packs" -> "Vende packs"
                        else -> {
                            if (listOf("Relacionamento sério", "Casual", "Ver no que dá", "Amizade / só conversar", "Vende packs").any { it.equals(rawObjective, ignoreCase = true) }) {
                                rawObjective
                            } else {
                                "Casual"
                            }
                        }
                    }

                    val phone = obj.optString("phone", obj.optString("whats", obj.optString("whatsapp", obj.optString("telefone", ""))))
                    val address = obj.optString("address", obj.optString("endereco", ""))
                    
                    val rawAvailability = obj.optString("availability", obj.optString("disponibilidade", "Solteira"))
                    val availability = when (rawAvailability.trim().lowercase()) {
                        "single" -> "Solteira"
                        "unavailable" -> "Indisponível"
                        "married" -> "Casada"
                        "lostcontact", "lost_contact" -> "Perdeu o contato"
                        "farloc", "far_location", "mora_longe" -> "Mora longe"
                        else -> {
                            if (listOf("Solteira", "Casada", "Indisponível", "Mora longe", "Perdeu o contato").any { it.equals(rawAvailability, ignoreCase = true) }) {
                                rawAvailability
                            } else {
                                "Solteira"
                            }
                        }
                    }

                    val status = obj.optString("status", "Ativa")
                    
                    // Parse negative traits as either a JSON array of strings or string
                    val negativesJson = obj.optJSONArray("negatives")
                    val rawNegatives = if (negativesJson != null) {
                        val list = mutableListOf<String>()
                        for (idx in 0 until negativesJson.length()) {
                            list.add(negativesJson.getString(idx))
                        }
                        list
                    } else {
                        val optStr = obj.optString("negatives", obj.optString("negativos", ""))
                        if (optStr.isNotEmpty()) optStr.split(",").map { it.trim() } else emptyList()
                    }

                    val translatedNegatives = rawNegatives.map { trait ->
                        val lower = trait.trim().lowercase()
                        when (lower) {
                            "endo", "endometriose" -> "endometriose"
                            "infertile", "sterile", "esteril", "estéril" -> "esteril"
                            "smoker", "fumante" -> "fumante"
                            "drugs", "drug", "drogas" -> "drogas"
                            "mental" -> "mental"
                            "feminist", "feminista" -> "feminista"
                            "lgbt", "ativista_lgbt" -> "ativista_lgbt"
                            "crazy", "unstable", "instavel", "instável" -> "instavel"
                            "exgp", "was_gp", "ex_gp" -> "ex_gp"
                            "manymen", "promiscuous", "rodada" -> "rodada"
                            "disabled", "pcd" -> "pcd"
                            "drinks", "alcohol", "bebebora" -> "bebebora"
                            "baggage", "party", "baladeira" -> "baladeira"
                            "obese", "weight", "obesa" -> "obesa"
                            "unemployed", "desempregada" -> "desempregada"
                            "crohn", "crohn/colite" -> "crohn"
                            "leftist", "red", "esquerdista" -> "esquerdista"
                            "vegan", "vegana" -> "vegana"
                            "debts", "dividas", "dívidas", "money" -> "dividas"
                            "ex", "ex_husband" -> "ex"
                            "family", "familia", "família" -> "familia"
                            "onlyfans" -> "onlyfans"
                            "religious", "religiosa" -> "religiosa"
                            else -> lower
                        }
                    }.filter { it.isNotEmpty() }.distinct().joinToString(",")

                    // Strict isGP classification: check if forceGP is enabled, isGP flag is set, or if name or notes indicate professional GP service
                    val isGP = forceGP || 
                               obj.optBoolean("isGP", obj.optBoolean("isGp", obj.optBoolean("gp", false))) ||
                               name.contains("GP", ignoreCase = true) ||
                               notes.contains("atendimento", ignoreCase = true) ||
                               notes.contains("Vênus Club", ignoreCase = true) ||
                               obj.has("gpPrice") || obj.has("preco") || obj.has("valor")
                               
                    val gpPrice = obj.optDouble("gpPrice", obj.optDouble("preco", obj.optDouble("valor", 0.0)))
                    
                    val rawCreatedAt = obj.optString("createdAt", "")
                    val createdAt: Long = if (rawCreatedAt.isNotEmpty()) {
                        try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val d = sdf.parse(rawCreatedAt)
                            d?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                    } else {
                        obj.optLong("createdAt", System.currentTimeMillis())
                    }

                    val originalIdInt = obj.optInt("id", 0)
                    val nameKey = name.trim().lowercase()

                    val targetPartnerId = if (existingPartnerByName.containsKey(nameKey)) {
                        existingPartnerByName[nameKey]!!.id
                    } else if (originalIdInt > 0 && originalIdInt !in existingPartnerIds) {
                        existingPartnerIds.add(originalIdInt)
                        maxPartnerId = maxOf(maxPartnerId, originalIdInt)
                        originalIdInt
                    } else {
                        maxPartnerId++
                        existingPartnerIds.add(maxPartnerId)
                        maxPartnerId
                    }

                    val partner = Partner(
                        id = targetPartnerId,
                        name = name,
                        location = location,
                        origin = origin,
                        age = age,
                        affinity = affinity,
                        photoUrl = photoUrl,
                        isFavorite = isFavorite,
                        notes = notes,
                        firstDate = firstDate,
                        rating = rating,
                        children = children,
                        myKids = myKids,
                        objective = objective,
                        phone = phone,
                        address = address,
                        availability = availability,
                        status = status,
                        negatives = translatedNegatives,
                        isGP = isGP,
                        gpPrice = gpPrice,
                        createdAt = createdAt
                    )

                    repository.insertPartner(partner)
                    existingPartnerByName[nameKey] = partner

                    if (originalIdStr.isNotEmpty()) {
                        idMap[originalIdStr] = targetPartnerId
                    }
                    if (originalIdInt != 0) {
                        idMap[originalIdInt.toString()] = targetPartnerId
                    }
                    idMap[nameKey] = targetPartnerId
                    
                    // Now parse nested encounters if any
                    val nestedEncounters = obj.optJSONArray("encounters") ?: obj.optJSONArray("encontros") ?: obj.optJSONArray("dates") ?: obj.optJSONArray("history")
                    if (nestedEncounters != null) {
                        for (j in 0 until nestedEncounters.length()) {
                            val encObj = nestedEncounters.getJSONObject(j)
                            importEncounterJson(encObj, targetPartnerId, name, isGP)
                        }
                    }
                }

                if (rootArray != null) {
                    for (i in 0 until rootArray.length()) {
                        val obj = rootArray.getJSONObject(i)
                        importPartnerJson(obj, forceGP = false)
                    }
                } else if (rootObject != null) {
                    val listKeysWithGPFlag = listOf(
                        Pair(listOf("women", "mulheres", "parceiras", "girls", "girlsList", "partnersList", "partners", "contatos"), false),
                        Pair(listOf("gps", "acompanhantes", "garotas", "profissionais", "atendimentos"), true)
                    )

                    for (pair in listKeysWithGPFlag) {
                        val keys = pair.first
                        val isGPFlag = pair.second
                        for (key in keys) {
                            if (rootObject.has(key)) {
                                val partnersArray = rootObject.optJSONArray(key)
                                if (partnersArray != null) {
                                    for (i in 0 until partnersArray.length()) {
                                        val obj = partnersArray.getJSONObject(i)
                                        importPartnerJson(obj, forceGP = isGPFlag)
                                    }
                                }
                            }
                        }
                    }

                    val encountersKeys = listOf("encontros", "dates", "encounters", "history", "historico", "appointments", "meetings")
                    var encontrosArray: JSONArray? = null
                    for (key in encountersKeys) {
                        if (rootObject.has(key)) {
                            encontrosArray = rootObject.getJSONArray(key)
                            break
                        }
                    }

                    if (encontrosArray != null) {
                        for (i in 0 until encontrosArray.length()) {
                            val obj = encontrosArray.getJSONObject(i)
                            importEncounterJson(obj, 0, "Desconhecida", false)
                        }
                    }
                }

                return@withContext true
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
    }

    fun importBackupFromUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonStr = inputStream?.bufferedReader()?.use { it.readText() }
                if (jsonStr != null) {
                    val success = parseAndImportJsonString(jsonStr)
                    withContext(Dispatchers.Main) {
                        if (success) {
                            Toast.makeText(context, "Sessão e backup importados com sucesso!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Arquivo inválido ou não compatível com o backup!", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Não foi possível abrir o arquivo!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro ao carregar arquivo de backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun importBackupFromText(context: android.content.Context, text: String, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            val success = parseAndImportJsonString(text)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Claude Web / HTML Data restaurado com sucesso!", Toast.LENGTH_LONG).show()
                    onFinished()
                } else {
                    Toast.makeText(context, "Erro: Certifique-se de ter colado uma estrutura válida de JSON ou dados do Claude!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            try {
                val file = File(getApplication<Application>().cacheDir, "agenda_h_backup.json")
                if (file.exists()) {
                    val text = file.readText()
                    val success = parseAndImportJsonString(text)
                    withContext(Dispatchers.Main) {
                        if (success) {
                            Toast.makeText(getApplication(), "Backup temporário restaurado!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(getApplication(), "Arquivo temporário inválido!", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    repository.populatePredefinedMockData()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Nenhum arquivo encontrado. O sistema gerou dados de demonstração!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Erro de validação no backup temporário!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun runSnapshot() {
        Toast.makeText(getApplication(), "Snapshot efetuado com sucesso!", Toast.LENGTH_SHORT).show()
    }

    fun wipeAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            Toast.makeText(getApplication(), "Todos os dados foram excluídos permanentemente.", Toast.LENGTH_LONG).show()
        }
    }

    // Helper Date Utilities
    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun calculateDaysBetween(startDateStr: String, endDateStr: String): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfBr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        val parse = { str: String ->
            try {
                if (str.contains("-")) sdf.parse(str) else sdfBr.parse(str)
            } catch (e: Exception) {
                null
            }
        }
        return try {
            val start = parse(startDateStr) ?: return 10
            val end = parse(endDateStr) ?: return 0
            val diffMs = end.time - start.time
            val diffDays = diffMs / (1000 * 60 * 60 * 24)
            if (diffDays < 0) 0 else diffDays.toInt()
        } catch (e: Exception) {
            10
        }
    }

    private fun saveAutoBackup(partners: List<Partner>, encounters: List<Encontro>) {
        if (!isRestoredOrChecked) return
        if (partners.isEmpty()) return
        
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val json = serializeBackup(partners, encounters)
                    prefs.edit().putString("auto_backup_json", json).apply()
                    val backupFile = File(getApplication<Application>().filesDir, "agenda_backup_auto.json")
                    backupFile.writeText(json, Charsets.UTF_8)
                    // Auto-mirror to "Agenda H" folder on user's device!
                    com.example.data.AgendaHFolderManager.saveBackupToAgendaH(getApplication(), json)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun serializeBackup(partners: List<Partner>, encounters: List<Encontro>): String {
        val root = org.json.JSONObject()
        
        val partnersArr = org.json.JSONArray()
        for (p in partners) {
            val pObj = org.json.JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("location", p.location)
                put("origin", p.origin)
                put("age", p.age)
                put("affinity", p.affinity)
                put("photoUrl", p.photoUrl)
                put("isFavorite", p.isFavorite)
                put("notes", p.notes)
                put("firstDate", p.firstDate)
                put("rating", p.rating)
                put("children", p.children)
                put("myKids", p.myKids)
                put("objective", p.objective)
                put("phone", p.phone)
                put("address", p.address)
                put("availability", p.availability)
                put("status", p.status)
                put("negatives", p.negatives)
                put("isGP", p.isGP)
                put("gpPrice", p.gpPrice)
                put("createdAt", p.createdAt)
            }
            partnersArr.put(pObj)
        }
        root.put("partners", partnersArr)
        
        val encountersArr = org.json.JSONArray()
        for (e in encounters) {
            val eObj = org.json.JSONObject().apply {
                put("id", e.id)
                put("partnerId", e.partnerId)
                put("partnerName", e.partnerName)
                put("date", e.date)
                put("time", e.time)
                put("motelCost", e.motelCost)
                put("typeOral", e.typeOral)
                put("typeAnal", e.typeAnal)
                put("typeVaginal", e.typeVaginal)
                put("typeCreampie", e.typeCreampie)
                put("typeFacial", e.typeFacial)
                put("typeSquirt", e.typeSquirt)
                put("typeDeepthroat", e.typeDeepthroat)
                put("typeFirstEncounter", e.typeFirstEncounter)
                put("typeVirginity", e.typeVirginity)
                put("notes", e.notes)
                put("hadSex", e.hadSex)
                put("isPregnancy", e.isPregnancy)
                put("rating", e.rating)
                put("countOral", e.countOral)
                put("countAnal", e.countAnal)
                put("countVaginal", e.countVaginal)
                put("countCreampie", e.countCreampie)
                put("countFacial", e.countFacial)
                put("countSquirt", e.countSquirt)
                put("countDeepthroat", e.countDeepthroat)
                put("countAnalCreampie", e.countAnalCreampie)
                put("isFirstEncontroSex", e.isFirstEncontroSex)
                put("isVirginityLost", e.isVirginityLost)
                put("typeAnalCreampie", e.typeAnalCreampie)
                put("isPregnancyMarked", e.isPregnancyMarked)
                put("gpSexoVaginal", e.gpSexoVaginal)
                put("gpOralSemCamisinha", e.gpOralSemCamisinha)
                put("gpSexoAnal", e.gpSexoAnal)
                put("gpBeijoNaBoca", e.gpBeijoNaBoca)
                put("gp69ComCamisinha", e.gp69ComCamisinha)
                put("gpMassagemErotica", e.gpMassagemErotica)
                put("gpDominacaoBdsm", e.gpDominacaoBdsm)
                put("gpBeijoGrego", e.gpBeijoGrego)
                put("gpInversaoFetiche", e.gpInversaoFetiche)
                put("gpGargantaProfunda", e.gpGargantaProfunda)
                put("gpFetichePe", e.gpFetichePe)
                put("gpFioTerra", e.gpFioTerra)
                put("gpGastoValor", e.gpGastoValor)
            }
            encountersArr.put(eObj)
        }
        root.put("encounters", encountersArr)
        
        return root.toString()
    }

    private suspend fun restoreBackup(jsonString: String): Boolean {
        if (jsonString.isEmpty()) return false
        return parseAndImportJsonString(jsonString)
    }

    fun saveBackupToAgendaH(context: android.content.Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val partners = repository.getAllPartnersList()
                    val encounters = repository.getAllEncontrosList()
                    if (partners.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Aviso: Não há parceiras cadastradas para salvar.", Toast.LENGTH_SHORT).show()
                        }
                        return@withContext
                    }
                    val json = serializeBackup(partners, encounters)
                    val (success, pathOrMsg) = com.example.data.AgendaHFolderManager.saveBackupToAgendaH(context, json)
                    withContext(Dispatchers.Main) {
                        if (success) {
                            Toast.makeText(context, "✅ Backup salvo com sucesso na pasta 'Agenda H'!\nLocal: $pathOrMsg", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "❌ $pathOrMsg", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Erro ao gravar na pasta Agenda H: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun restoreBackupFromAgendaH(context: android.content.Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val (jsonContent, pathOrMsg) = com.example.data.AgendaHFolderManager.readBackupFromAgendaH(context)
                if (jsonContent.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "⚠️ $pathOrMsg", Toast.LENGTH_LONG).show()
                    }
                    return@withContext
                }
                val success = parseAndImportJsonString(jsonContent)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(context, "✅ Dados recuperados com sucesso da pasta 'Agenda H'!\nArquivo: $pathOrMsg", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "❌ Falha ao processar arquivo da pasta 'Agenda H'.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // --- COACH INVISÍVEL IA (SEDUCTION COACH SCREEN STATE & CONTROLLER) ---
    var coachAnalysisResult by androidx.compose.runtime.mutableStateOf(prefs.getString("last_coach_analysis", "") ?: "")
    var coachSuggestedResponses by androidx.compose.runtime.mutableStateOf(prefs.getString("last_suggested_responses", "") ?: "")
    var coachSuggestedOptions by androidx.compose.runtime.mutableStateOf<List<String>>(
        parseCoachSuggestedOptions(prefs.getString("last_suggested_responses", "") ?: "")
    )

    init {
        com.example.FloatingCoachManager.onAnalysisResultUpdated = { analysis, responses ->
            coachAnalysisResult = analysis
            coachSuggestedResponses = responses
            coachSuggestedOptions = parseCoachSuggestedOptions(responses)
        }
    }
    var isCoachLoading by androidx.compose.runtime.mutableStateOf(false)
    var selectedCoachImageUris by androidx.compose.runtime.mutableStateOf<List<android.net.Uri>>(emptyList())
    var coachCustomPrompt by androidx.compose.runtime.mutableStateOf("")
    private var _coachApiKeyInput = androidx.compose.runtime.mutableStateOf(prefs.getString("coach_api_key", "") ?: "")
    var coachApiKeyInput: String
        get() = _coachApiKeyInput.value
        set(value) {
            _coachApiKeyInput.value = value
            prefs.edit().putString("coach_api_key", value).apply()
        }

    fun addCoachImage(uri: android.net.Uri) {
        selectedCoachImageUris = selectedCoachImageUris + uri
    }

    fun removeCoachImage(uri: android.net.Uri) {
        selectedCoachImageUris = selectedCoachImageUris.filter { it != uri }
    }

    fun clearCoachImages() {
        selectedCoachImageUris = emptyList()
    }

    fun clearCoachResult() {
        coachAnalysisResult = ""
        coachSuggestedResponses = ""
        coachSuggestedOptions = emptyList()
    }

    // --- MEU PERFIL ANALYZER & DESCRIÇÃO GENERATOR ---
    var myHobbiesInput by androidx.compose.runtime.mutableStateOf("")
    var myTargetInput by androidx.compose.runtime.mutableStateOf("")
    var myDescriptionStyle by androidx.compose.runtime.mutableStateOf("Misterioso & Provocador")
    var profileAnalysisResult by androidx.compose.runtime.mutableStateOf("")
    var generatedBiosResult by androidx.compose.runtime.mutableStateOf("")
    var isProfileLoading by androidx.compose.runtime.mutableStateOf(false)
    var selectedMyProfileImageUris by androidx.compose.runtime.mutableStateOf<List<android.net.Uri>>(emptyList())

    fun addMyProfileImage(uri: android.net.Uri) {
        selectedMyProfileImageUris = selectedMyProfileImageUris + uri
    }

    fun removeMyProfileImage(uri: android.net.Uri) {
        selectedMyProfileImageUris = selectedMyProfileImageUris.filter { it != uri }
    }

    fun clearMyProfileImages() {
        selectedMyProfileImageUris = emptyList()
    }

    fun clearProfileResult() {
        profileAnalysisResult = ""
        generatedBiosResult = ""
    }

    // --- ASSISTENTE INTELIGENTE & DIAGNÓSTICO DA AGENDA (GEMINI FLASH-LATEST) ---
    var assistantQuery by androidx.compose.runtime.mutableStateOf("")
    var assistantAnswer by androidx.compose.runtime.mutableStateOf(prefs.getString("last_assistant_answer", "") ?: "")
    var isAssistantLoading by androidx.compose.runtime.mutableStateOf(false)

    var agendaAnalysisResult by androidx.compose.runtime.mutableStateOf(prefs.getString("last_agenda_analysis", "") ?: "")
    var isAgendaAnalysisLoading by androidx.compose.runtime.mutableStateOf(false)

    fun clearAssistantAnswer() {
        assistantAnswer = ""
        prefs.edit().remove("last_assistant_answer").apply()
    }

    fun clearAgendaAnalysis() {
        agendaAnalysisResult = ""
        prefs.edit().remove("last_agenda_analysis").apply()
    }

    fun runIntelligentAgendaAnalysis(context: android.content.Context) {
        viewModelScope.launch {
            isAgendaAnalysisLoading = true
            agendaAnalysisResult = ""
            withContext(Dispatchers.IO) {
                try {
                    val partners = repository.getAllPartnersList()
                    val encounters = repository.getAllEncontrosList()

                    if (partners.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            agendaAnalysisResult = "⚠️ Nenhuma parceira cadastrada na sua Agenda H ainda.\n\nCadastre mulheres e encontros para que o Gemini flash-latest possa elaborar um diagnóstico estratégico completo da sua vida amorosa e táticas de conquista."
                            isAgendaAnalysisLoading = false
                        }
                        return@withContext
                    }

                    val totalPartners = partners.size
                    val totalGps = partners.count { it.isGP }
                    val totalNonGps = totalPartners - totalGps
                    val totalEncontros = encounters.size
                    val totalSex = encounters.count { it.hadSex }
                    val totalSpentMotel = encounters.sumOf { it.motelCost }
                    val totalSpentGp = encounters.sumOf { it.gpGastoValor }

                    val partnerDetails = partners.joinToString("\n") { p ->
                        val pEnc = encounters.filter { it.partnerId == p.id }
                        val sexCount = pEnc.count { it.hadSex }
                        val lastDate = pEnc.mapNotNull { it.date }.filter { it.isNotBlank() }.sortedDescending().firstOrNull() ?: p.firstDate
                        val abstinenceDays = if (lastDate.isNotBlank()) calculateDaysBetween(lastDate, getTodayDateString()) else 999
                        val classification = com.example.data.WomanCalculator.calculateCategory(p, pEnc)
                        
                        "• ${p.name} (Id: ${p.id}, Categoria: ${classification.displayName}, GP: ${p.isGP}, Nota: ${p.rating}, Afinidade: ${p.affinity}%, Encontros: ${pEnc.size}, Sexo: $sexCount vezes, Último: ${if (lastDate.isNotBlank()) lastDate else "nenhum"}, Abstinência: $abstinenceDays dias, Negativos: ${p.negatives.ifEmpty { "nenhum" }})"
                    }

                    val prompt = """
                        Você é o Consultor Estratégico Sênior de Relacionamentos e Inteligência Social do aplicativo Agenda H.
                        Analise os dados reais da agenda do usuário com profundidade tática, discernimento psicológico e clareza cirúrgica.
                        
                        PANORAMA ATUAL DA AGENDA:
                        - Total de contatos cadastrados: $totalPartners ($totalNonGps casuais/relacionamento, $totalGps serviços/GP)
                        - Total de encontros realizados: $totalEncontros (com sexo: $totalSex)
                        - Investimento financeiro registrado: R$ ${String.format(Locale.GERMANY, "%.2f", totalSpentMotel + totalSpentGp)}
                        
                        DETALHE DAS PARCEIRAS:
                        $partnerDetails
                        
                        ESTRUTURE SUA RESPOSTA DA SEGUINTE FORMA:
                        1. 📊 DIAGNÓSTICO GERAL DA SUA VIDA AFETIVA & SOCIAL
                        (Análise global de dinâmica, ritmo de encontros e equilíbrio entre esforço e retorno)
                        
                        2. 🏆 ANÁLISE DE POTENCIAIS & CATEGORIZAÇÃO TÁTICA
                        (Destaque quem tem real potencial para Futura Esposa, Namorada ou Ficante Assídua e aponte justificativas claras com base nos encontros e abstinência)
                        
                        3. ⚠️ PONTOS DE ATENÇÃO & ALERTA VERMELHO
                        (Alertas sobre parceiras com muitos dias de abstinência, flags negativas acumuladas ou gastos desproporcionais)
                        
                        4. 🎯 PLANO DE AÇÃO PARA OS PRÓXIMOS ENCONTROS
                        (3 conselhos táticos imediatos sobre com quem marcar encontro esta semana e quais estratégias adotar)
                    """.trimIndent()

                    val result = com.example.data.GeminiApiClient.generateContent(
                        prompt = prompt,
                        userKey = coachApiKeyInput,
                        systemInstruction = "Você é um assistente tático de relacionamentos masculino de alta precisão e linguagem descontraída, madura e assertiva."
                    )

                    withContext(Dispatchers.Main) {
                        result.onSuccess { text ->
                            agendaAnalysisResult = text
                            prefs.edit().putString("last_agenda_analysis", text).apply()
                        }.onFailure { error ->
                            agendaAnalysisResult = error.localizedMessage ?: "Erro ao gerar análise da agenda com o Gemini."
                        }
                        isAgendaAnalysisLoading = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        agendaAnalysisResult = "❌ Falha ao processar análise da agenda:\n${e.localizedMessage ?: "Erro desconhecido"}"
                        isAgendaAnalysisLoading = false
                    }
                }
            }
        }
    }

    fun askGeminiAssistant(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            isAssistantLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val partners = repository.getAllPartnersList()
                    val partnerNames = partners.take(15).joinToString(", ") { "${it.name} (${it.status})" }
                    val contextInfo = "Contexto do usuário: Possui ${partners.size} parceiras cadastradas na Agenda H ($partnerNames)."
                    
                    val prompt = """
                        Contexto atual do usuário no app:
                        $contextInfo
                        
                        Pergunta do usuário para o Assistente:
                        "$query"
                        
                        Responda com autoridade, carisma, dicas acionáveis e conselhos claros de sedução e dinâmica de relacionamentos.
                    """.trimIndent()

                    val result = com.example.data.GeminiApiClient.generateContent(
                        prompt = prompt,
                        userKey = coachApiKeyInput,
                        systemInstruction = "Você é o Assistente Inteligente Oficial do app Agenda H, especialista em sedução, comunicação com mulheres, atração e psicologia feminina."
                    )

                    withContext(Dispatchers.Main) {
                        result.onSuccess { answer ->
                            assistantAnswer = answer
                            prefs.edit().putString("last_assistant_answer", answer).apply()
                        }.onFailure { error ->
                            assistantAnswer = error.localizedMessage ?: "Erro de conexão com o Gemini."
                        }
                        isAssistantLoading = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        assistantAnswer = "❌ Erro ao consultar o Assistente Inteligente:\n${e.localizedMessage ?: "Verifique sua conexão."}"
                        isAssistantLoading = false
                    }
                }
            }
        }
    }

    fun analyzeCoachPrint(context: android.content.Context, bitmaps: List<android.graphics.Bitmap>, textPrompt: String) {
        viewModelScope.launch {
            isCoachLoading = true
            coachAnalysisResult = ""
            coachSuggestedResponses = ""
            coachSuggestedOptions = emptyList()

            val basePrompt = """
                Você é o "Coach Invisível IA", mestre em sedução estratégica, comunicação persuasiva e psicologia feminina de alto nível.
                O usuário enviou prints de perfis e conversas reais de aplicativos de namoro (Tinder, Bumble, Instagram, WhatsApp, etc.).

                DIRETRIZES FUNDAMENTAIS & FOCO ABSOLUTO:
                1. O foco de todas as respostas sugeridas DEVE SER SEMPRE em chamar a atenção da mulher de forma POSITIVA, magnética e intrigante.
                2. O objetivo primordial é conseguir agendar um encontro principalmente sexual o mais breve possível.
                3. Conduza tudo de maneira SUTIL, ELEGANTE E POSITIVA — NUNCA ofenda, insulte ou use vulgaridade barata com a mulher. A sedução deve ser refinada, estimulando cumplicidade, subtexto sensual inteligente e desejo mútuo.
                4. Adapte a resposta estritamente aos gatilhos de sedução, estilo de humor e padrões de resposta que ela demonstrou nos prints e no texto da conversa.
                5. Você DEVE fornecer EXATAMENTE 3 SUGESTÕES DE RESPOSTAS DISTINTAS para o usuário copiar desse app e colar diretamente no app de namoro.

                ESTRUTURA DE RESPOSTA OBRIGATÓRIA:
                Separe a resposta EXATAMENTE com o delimitador: ===RESPOSTAS_SUGERIDAS_SECAO===

                SEÇÃO 1 (Anterior ao delimitador):
                🎯 ANÁLISE PSICOLÓGICA & GATILHOS DA MULHER
                - Leitura do perfil dela e tom da conversa
                - Gatilhos de atração identificados nos prints (ex: ironia leve, sofisticação, validação lúdica, desafio)
                - Nível de interesse atual (0 a 10) e melhor ângulo de ataque

                ⚔️ ESTRATÉGIA DE CONDUÇÃO RÁPIDA PARA O ENCONTRO
                - Roteiro tático para transição da conversa do app para o WhatsApp ou encontro íntimo
                - Como conduzir com sutileza sem parecer desesperado

                ===RESPOSTAS_SUGERIDAS_SECAO===
                SEÇÃO 2 (Após o delimitador - exatamente 3 opções divididas pelo separador ===DIVISOR_RESPOSTA===):

                OPÇÃO 1: ABORDAGEM CHARMOSA & CURIOSIDADE
                [Texto da resposta 1 pronto para copiar e colar no app de namoro. Tom descontraído, magnético, que chama atenção positivamente e faz ela responder na hora.]
                ===DIVISOR_RESPOSTA===
                OPÇÃO 2: PROVOCAÇÃO SUTIL & TENSÃO VELADA
                [Texto da resposta 2 pronto para copiar e colar no app de namoro. Provocação charmosa e inteligente, criando cumplicidade e subtexto de química/atração sem ser vulgar.]
                ===DIVISOR_RESPOSTA===
                OPÇÃO 3: CONVITE DIRETO PARA ENCONTRO ÍNTIMO
                [Texto da resposta 3 pronto para copiar e colar no app de namoro. Proposta irresistível, leve e segura de encontro a dois com rápida escalada para privacidade.]
            """.trimIndent()
            
            val finalPrompt = "$basePrompt\n\nContexto da conversa enviado pelo Usuário:\n$textPrompt"

            withContext(Dispatchers.IO) {
                val result = com.example.data.GeminiApiClient.generateContent(
                    prompt = finalPrompt,
                    userKey = coachApiKeyInput,
                    bitmaps = bitmaps
                )

                withContext(Dispatchers.Main) {
                    result.onSuccess { textResult ->
                        val parts = textResult.split("===RESPOSTAS_SUGERIDAS_SECAO===")
                        if (parts.size >= 2) {
                            coachAnalysisResult = parts[0].trim()
                            coachSuggestedResponses = parts[1].trim()
                        } else {
                            coachAnalysisResult = textResult
                            coachSuggestedResponses = textResult
                        }
                        coachSuggestedOptions = parseCoachSuggestedOptions(coachSuggestedResponses)
                        prefs.edit()
                            .putString("last_coach_analysis", coachAnalysisResult)
                            .putString("last_suggested_responses", coachSuggestedResponses)
                            .apply()
                    }.onFailure { error ->
                        coachAnalysisResult = error.localizedMessage ?: "Erro ao analisar print com o Gemini."
                        coachSuggestedResponses = ""
                        coachSuggestedOptions = emptyList()
                    }
                    isCoachLoading = false
                }
            }
        }
    }

    fun analyzeMyProfile(context: android.content.Context, bitmaps: List<android.graphics.Bitmap>, style: String, hobbiesText: String, targetText: String) {
        viewModelScope.launch {
            isProfileLoading = true
            profileAnalysisResult = ""
            generatedBiosResult = ""

            val hobbies = hobbiesText.trim().ifEmpty { "Tecnologia, leitura, academia, viagens" }
            val target = targetText.trim().ifEmpty { "Mulheres inteligentes, bem-humoradas e decididas" }

            val promptText = """
                Você é o "Treinador de Perfil H & Coach de Aplicativos de Namoro", um especialista internacional em atração digital e engenharia social tática para plataformas de namoro (Tinder, Bumble, Badoo, Happn, Facebook Namoro etc.).
                O usuário deseja analisar e otimizar seu perfil de namoro para atrair mais mulheres e maximizar de forma absoluta sua conversão de "match" para "conversa produtiva".

                O usuário selecionou o seguinte estilo de descrição preferido para gerar suas novas bios: "$style".
                Hobbies e interesses informados pelo usuário sobre si mesmo: "$hobbies"
                Público-alvo / Objetivo de atração de mulheres desejado pelo usuário: "$target"

                Você recebeu ${bitmaps.size} fotos enviadas pelo usuário (se houver imagens incluídas).
                Analise as fotos do perfil dele (se fornecidas), aponte problemas visuais comuns (ex: iluminação ruim, selfies sem graça, falta de expressão, excesso de fotos em grupo ou de óculos escuros). Forneça dicas impecáveis e cirúrgicas para fotos que GERAM MATCH DE ALTO VALOR de acordo com as diretrizes táticas.

                Em seguida, gere duas opções de descrições brilhantes e inovadoras estruturadas sob o estilo selecionado ($style), totalmente customizadas com base nos hobbies ($hobbies) e direcionadas com foco em atrair o público ideal ($target). Evite jargões comuns e "clichês" batidos de aplicativo de namoro. Seja extremamente sagaz, atraente e instigante.

                Responda dividindo o parecer exatamente em duas seções usando o separador exclusivo "===GERADOR_DESC_SECAO===":

                Nesta primeira seção, coloque a análise de fotos e dicas:
                🎯 ANÁLISE DE FOTOS & MELHORIAS TÁTICAS
                [Dicas concretas para as fotos que ele enviou ou sugestões de novas poses, enquadramentos e cenários campeões de conversão com as mulheres, usando os hobbies: $hobbies no plano visual]

                ===GERADOR_DESC_SECAO===
                Na segunda seção (após o delimitador silencioso), coloque apenas as descrições geradas:
                💬 BIO / DESCRIÇÕES PRONTAS DE ALTO RETORNO

                Opção Principal (Customizada e Focada no Estilo $style):
                "[Coloque aqui a primeira bio deslumbrante pronta para copiar]"

                Opção Alternativa (Estratégia Abridores Indiretos):
                "[Coloque aqui a segunda bio brilhante e intrigante]"
            """.trimIndent()

            withContext(Dispatchers.IO) {
                val result = com.example.data.GeminiApiClient.generateContent(
                    prompt = promptText,
                    userKey = coachApiKeyInput,
                    bitmaps = bitmaps
                )

                withContext(Dispatchers.Main) {
                    result.onSuccess { textResult ->
                        val parts = textResult.split("===GERADOR_DESC_SECAO===")
                        if (parts.size >= 2) {
                            profileAnalysisResult = parts[0].trim()
                            generatedBiosResult = parts[1].trim()
                        } else {
                            profileAnalysisResult = textResult
                            generatedBiosResult = "💬 Veja as descrições prontas integradas na análise acima."
                        }
                    }.onFailure { error ->
                        profileAnalysisResult = error.localizedMessage ?: "Erro ao analisar perfil com o Gemini."
                        generatedBiosResult = ""
                    }
                    isProfileLoading = false
                }
            }
        }
    }

    fun generateSmartDateWithGemini(
        woman: Partner?,
        locationMode: String,
        category: String,
        nearbyPlaces: List<LocationService.NearbyPlace>
    ) {
        viewModelScope.launch {
            isSmartDateAiLoading = true
            smartDateAiResult = ""

            val targetLocationDescription = when (locationMode) {
                "Casa dela" -> {
                    val addr = woman?.address?.ifBlank { "Endereço cadastrado da parceira" } ?: "Residência da mulher"
                    "Casa dela ($addr)"
                }
                else -> {
                    "Minha localização (${userGpsAddress.ifBlank { "Minha posição atual / GPS" }})"
                }
            }

            val womanInfo = if (woman != null) {
                "Parceira selecionada: ${woman.name}, Idade: ${woman.age} anos, Objetivo: ${woman.objective}, Status: ${woman.status}, Endereço: ${woman.address.ifEmpty { "Não informado" }}, Notas/Perfil: ${woman.notes.ifEmpty { "Sem notas adicionais" }}, Alertas/Preferências: ${woman.negatives.ifEmpty { "Nenhum" }}."
            } else {
                "Parceira: Perfil geral / Encontro casual."
            }

            val placesSummary = if (nearbyPlaces.isNotEmpty()) {
                "Locais reais mapeados via GPS em até 7km:\n" +
                nearbyPlaces.take(6).joinToString("\n") { "• ${it.name} (${it.category}) - ${it.distanceInKm} km de distância - Faixa: ${it.priceBracket} - Endereço: ${it.address}" }
            } else {
                "Ponto de referência marcado: $targetLocationDescription."
            }

            val prompt = """
                Você é o Estrategista Oficial de Dates do app Agenda H, alimentado pelo Google Gemini (${com.example.data.GeminiApiClient.MODEL_NAME}).
                
                O usuário precisa de SUGESTÕES DE LOCAIS DE DATE e roteiro de conquista.
                
                PARÂMETROS ESSENCIAIS:
                - Aba de Localização Marcada: $locationMode
                - Ponto de Referência: $targetLocationDescription
                - RESTRIÇÃO INEGOCIÁVEL DE RAIO: RIGOROSAMENTE DENTRO DE ATÉ 7 KM da localização marcada ($targetLocationDescription). Nenhum local sugerido pode exceder 7 km de distância!
                - Categoria Selecionada: $category (Ex: Motel, Date Barato, Shopping, Ostentação)
                - $womanInfo
                - $placesSummary
                
                MISSÃO:
                Atuar diretamente na aba '$locationMode' e sugerir locais para date de acordo com a categoria selecionada '$category' estritamente dentro do raio de 7 km da localização marcada ($targetLocationDescription).
                
                ESTRUTURA DE RESPOSTA OBRIGATÓRIA:
                
                1. 📍 SUGESTÕES DE LOCAIS DE DATE (Categoria: $category • Raio máx 7km de $locationMode)
                Apresente 3 a 4 opções específicas de locais para o date adequados à categoria '$category' (seja motel com suíte privativa, gastrobar/café em date barato, lounge/bistrô em shopping ou restaurante requintado em ostentação) com distância estimada estritamente menor ou igual a 7 km:
                • [Nome do Local 1] - Distância estimada: <= 7km - Estilo e diferencial - Por que é ideal para este date
                • [Nome do Local 2] - Distância estimada: <= 7km - Estilo e diferencial - Por que é ideal para este date
                • [Nome do Local 3] - Distância estimada: <= 7km - Estilo e diferencial - Por que é ideal para este date
                • [Nome do Local 4] - Distância estimada: <= 7km - Estilo e diferencial - Por que é ideal para este date
                
                2. 💬 CONVITES PRONTOS PARA WHATSAPP (Copiar & Colar)
                2 opções calibradas de convite para WhatsApp propondo o date de forma sutil, positiva e atraente.
                
                3. 🧠 GATILHOS DE CONVERSA & TÓPICOS IDEAIS
                3 assuntos e perguntas instigantes para usar durante o encontro e despertar química sem deixar a conversa morrer.
                
                4. 🔥 TRANSIÇÃO & ESCALADA FÍSICA PARA O MOTEL / INTIMIDADE
                Roteiro sutil, gradual e elegante para transitar do date para um ambiente íntimo (casa ou motel) com alta taxa de aceitação.
            """.trimIndent()

            withContext(Dispatchers.IO) {
                val result = com.example.data.GeminiApiClient.generateContent(
                    prompt = prompt,
                    userKey = coachApiKeyInput
                )
                withContext(Dispatchers.Main) {
                    result.onSuccess { responseText ->
                        smartDateAiResult = responseText
                    }.onFailure { error ->
                        smartDateAiResult = error.localizedMessage ?: "Erro ao gerar sugestão de date com Gemini."
                    }
                    isSmartDateAiLoading = false
                }
            }
        }
    }

    fun regeneratePredefinedMockData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.populatePredefinedMockData()
            }
            Toast.makeText(getApplication(), "Dados de demonstração gerados com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun parseCoachSuggestedOptions(rawText: String): List<String> {
            if (rawText.isBlank()) return emptyList()
            if (rawText.contains("===DIVISOR_RESPOSTA===")) {
                val items = rawText.split("===DIVISOR_RESPOSTA===")
                    .map { cleanResponseOption(it) }
                    .filter { it.isNotBlank() }
                if (items.isNotEmpty()) return items.take(3)
            }
            val regex = Regex("(?i)(OPÇÃO|Opção|SUGESTÃO|Sugestão|Opcao)\\s*\\d*\\s*[:\\-]?[^\\n]*\\n?")
            val splits = rawText.split(regex).map { it.trim().trim('"', '“', '”') }.filter { it.length > 5 }
            if (splits.size >= 2) {
                return splits.take(3)
            }
            val quoteMatches = Regex("\"([^\"]{6,})\"|“([^”]{6,})”").findAll(rawText).map {
                it.groupValues[1].ifEmpty { it.groupValues[2] }.trim()
            }.toList()
            if (quoteMatches.size >= 2) {
                return quoteMatches.take(3)
            }
            return listOf(rawText.trim())
        }

        private fun cleanResponseOption(text: String): String {
            return text.replace(Regex("^(OPÇÃO|Opção|SUGESTÃO|Sugestão|Opcao)\\s*\\d*\\s*[:\\-]?[^\\n]*\\n?"), "")
                .trim()
                .trim('"', '“', '”', '\'', '`')
                .trim()
        }
    }
}
