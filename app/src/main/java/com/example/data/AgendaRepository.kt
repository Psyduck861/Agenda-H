package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgendaRepository(
    private val partnerDao: PartnerDao,
    private val encontroDao: EncontroDao
) {
    val allPartners: Flow<List<Partner>> = partnerDao.getAllPartners()
    val allEncontros: Flow<List<Encontro>> = encontroDao.getAllEncontros()

    suspend fun getAllPartnersList(): List<Partner> = partnerDao.getAllPartnersList()
    suspend fun getAllEncontrosList(): List<Encontro> = encontroDao.getAllEncontrosList()

    suspend fun getPartnerById(id: Int): Partner? = partnerDao.getPartnerById(id)

    suspend fun insertPartner(partner: Partner): Long = partnerDao.insertPartner(partner)

    suspend fun updatePartner(partner: Partner) = partnerDao.updatePartner(partner)

    suspend fun deletePartner(partner: Partner) {
        partnerDao.deletePartner(partner)
    }

    suspend fun deletePartnerById(id: Int) = partnerDao.deletePartnerById(id)

    fun getEncontrosForPartner(partnerId: Int): Flow<List<Encontro>> =
        encontroDao.getEncontrosForPartner(partnerId)

    suspend fun insertEncontro(encontro: Encontro): Long {
        val partner = partnerDao.getPartnerById(encontro.partnerId)
        val finalEncontro = if (partner != null) {
            // Ensure firstDate of partner is set if not already present
            if (partner.firstDate.isEmpty()) {
                partnerDao.updatePartner(partner.copy(firstDate = encontro.date))
            }
            encontro.copy(partnerName = partner.name)
        } else {
            encontro
        }
        val sanitizedEncontro = if (partner != null && !partner.isGP) {
            val name = partner.name.trim()
            val isAllowed = name.equals("Juliete", ignoreCase = true) || 
                            name.equals("Vanderléia", ignoreCase = true) || 
                            name.equals("Vanderleia", ignoreCase = true)
            if (!isAllowed) {
                finalEncontro.copy(
                    typeCreampie = false,
                    countCreampie = 0
                )
            } else {
                finalEncontro
            }
        } else {
            finalEncontro
        }
        return encontroDao.insertEncontro(sanitizedEncontro)
    }

    suspend fun updateEncontro(encontro: Encontro) {
        val partner = partnerDao.getPartnerById(encontro.partnerId)
        val sanitizedEncontro = if (partner != null && !partner.isGP) {
            val name = partner.name.trim()
            val isAllowed = name.equals("Juliete", ignoreCase = true) || 
                            name.equals("Vanderléia", ignoreCase = true) || 
                            name.equals("Vanderleia", ignoreCase = true)
            if (!isAllowed) {
                encontro.copy(
                    typeCreampie = false,
                    countCreampie = 0
                )
            } else {
                encontro
            }
        } else {
            encontro
        }
        encontroDao.updateEncontro(sanitizedEncontro)
    }

    suspend fun deleteEncontro(encontro: Encontro) = encontroDao.deleteEncontro(encontro)

    suspend fun deleteEncontroById(id: Int) = encontroDao.deleteEncontroById(id)

    suspend fun clearAllData() {
        encontroDao.clearAllEncontros()
        encontroDao.clearAllPartners()
    }

    suspend fun populateMockDataIfEmpty(partnersList: List<Partner>, encuentrosList: List<Encontro>) {
        // Checked in ViewModel
    }

    suspend fun populatePredefinedMockData() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        clearAllData()

        val pIdMap = mutableMapOf<String, Int>()

        addStandardPartners(pIdMap)
        addGPs(pIdMap)
        addStandardEncounters(pIdMap)
        addGPEncounters(pIdMap)
    }

    private suspend fun addPartner(
        pIdMap: MutableMap<String, Int>,
        oldId: String,
        name: String,
        age: Int,
        children: Int,
        origin: String,
        objective: String,
        location: String,
        phone: String,
        address: String,
        availability: String,
        createdAtStr: String,
        firstDate: String,
        status: String,
        negatives: String = "",
        isGP: Boolean = false,
        gpPrice: Double = 0.0
    ): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cTime = try { sdf.parse(createdAtStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
        
        val cleanLocation = when (location.trim().lowercase()) {
            "nearby" -> "Mesma cidade / <5km"
            "region" -> "Município próximo (5-20km)"
            "samestate" -> "Mesmo estado (>20km)"
            "otherstate" -> "Outro estado"
            "abroad" -> "Exterior"
            else -> {
                if (listOf("Mesma cidade / <5km", "Município próximo (5-20km)", "Mesmo estado (>20km)", "Outro estado", "Exterior").any { it.equals(location, ignoreCase = true) }) {
                    location
                } else {
                    "Mesma cidade / <5km"
                }
            }
        }
        val cleanObjective = when (objective.trim().lowercase()) {
            "serious" -> "Relacionamento sério"
            "casual" -> "Casual"
            "maybe" -> "Ver no que dá"
            "friendship", "chat" -> "Amizade / só conversar"
            "packs" -> "Vende packs"
            else -> {
                if (listOf("Relacionamento sério", "Casual", "Ver no que dá", "Amizade / só conversar", "Vende packs").any { it.equals(objective, ignoreCase = true) }) {
                    objective
                } else {
                    "Casual"
                }
            }
        }
        val cleanAvailability = when (availability.trim().lowercase()) {
            "single" -> "Solteira"
            "unavailable" -> "Indisponível"
            "married" -> "Casada"
            "lostcontact" -> "Perdeu o contato"
            "farloc" -> "Mora longe"
            else -> {
                if (listOf("Solteira", "Casada", "Indisponível", "Mora longe", "Perdeu o contato").any { it.equals(availability, ignoreCase = true) }) {
                    availability
                } else {
                    "Solteira"
                }
            }
        }
        val cleanStatus = if (status.trim().lowercase() == "active" || status.trim().lowercase() == "ativa") "Ativa" else "Inativa"

        val p = Partner(
            name = name,
            location = cleanLocation,
            origin = origin,
            age = age,
            affinity = if (isGP) 80 else 75,
            firstDate = firstDate,
            rating = if (isGP) 8.5 else 8.0,
            children = children,
            myKids = 0,
            objective = cleanObjective,
            phone = phone,
            address = address,
            availability = cleanAvailability,
            status = cleanStatus,
            negatives = negatives,
            isGP = isGP,
            gpPrice = gpPrice,
            createdAt = cTime
        )
        val newId = partnerDao.insertPartner(p).toInt()
        pIdMap[oldId] = newId
        return newId
    }

    private suspend fun addP(
        pIdMap: MutableMap<String, Int>,
        oldId: String, name: String, age: Int, children: Int, origin: String, objective: String, location: String, phone: String, address: String, availability: String, createdAtStr: String, firstDate: String, status: String, negatives: String = ""
    ): Int {
        return addPartner(pIdMap, oldId, name, age, children, origin, objective, location, phone, address, availability, createdAtStr, firstDate, status, negatives, isGP = false, gpPrice = 0.0)
    }

    private suspend fun addGP(
        pIdMap: MutableMap<String, Int>,
        oldId: String, name: String, establishment: String, city: String, phone: String, notes: String, createdAtStr: String, gpPrice: Double
    ): Int {
        val fullAddr = if (establishment.isNotEmpty()) "$establishment, $city" else city
        return addPartner(pIdMap, oldId, name, 25, 0, "Safe", "Casual", "nearby", phone, fullAddr, "Solteira", createdAtStr, "", "active", "", isGP = true, gpPrice = gpPrice)
    }

    private suspend fun addEncontro(
        partnerId: Int,
        partnerName: String,
        date: String,
        hadSex: Boolean,
        rating: Double = 5.0,
        notes: String = "",
        vaginal: Int = 0,
        anal: Int = 0,
        creampie: Int = 0,
        analCreampie: Int = 0,
        squirt: Int = 0,
        facial: Int = 0,
        firstEncounter: Boolean = false,
        oral: Int = 0,
        gpIsGP: Boolean = false,
        gpPrice: Double = 0.0,
        isVirginityLost: Boolean = false
    ) {
        val isAllowed = gpIsGP || 
                        partnerName.equals("Juliete", ignoreCase = true) || 
                        partnerName.equals("Vanderléia", ignoreCase = true) || 
                        partnerName.equals("Vanderleia", ignoreCase = true)
        val finalCreampie = if (isAllowed) creampie else 0

        val encontro = Encontro(
            partnerId = partnerId,
            partnerName = partnerName,
            date = date,
            time = "22:00",
            motelCost = if (gpIsGP) gpPrice else if (hadSex) { if (partnerName.equals("Juliete", ignoreCase = true)) 0.0 else 100.0 } else 0.0,
            typeOral = oral > 0,
            typeAnal = anal > 0,
            typeVaginal = vaginal > 0,
            typeCreampie = finalCreampie > 0,
            typeFacial = facial > 0,
            typeSquirt = squirt > 0,
            typeFirstEncounter = firstEncounter,
            typeVirginity = isVirginityLost,
            typeAnalCreampie = analCreampie > 0,
            notes = notes,
            hadSex = hadSex,
            rating = rating,
            countOral = oral,
            countAnal = anal,
            countVaginal = vaginal,
            countCreampie = finalCreampie,
            countFacial = facial,
            countSquirt = squirt,
            countAnalCreampie = analCreampie,
            isFirstEncontroSex = firstEncounter,
            isVirginityLost = isVirginityLost,
            gpSexoVaginal = gpIsGP && vaginal > 0,
            gpOralSemCamisinha = gpIsGP && oral > 0,
            gpSexoAnal = gpIsGP && anal > 0,
            gpBeijoNaBoca = gpIsGP && finalCreampie > 0,
            gpGastoValor = if (gpIsGP) gpPrice else 0.0
        )
        encontroDao.insertEncontro(encontro)
    }

    private suspend fun addGPEncounter(
        pIdMap: Map<String, Int>,
        gpOldId: String,
        date: String,
        vaginal: Boolean,
        anal: Boolean,
        oral: Boolean,
        price: Double,
        notes: String = ""
    ) {
        val pId = pIdMap[gpOldId] ?: return
        val partner = partnerDao.getPartnerById(pId) ?: return
        addEncontro(
            partnerId = pId,
            partnerName = partner.name,
            date = date,
            hadSex = true,
            rating = 8.0,
            notes = notes,
            vaginal = if (vaginal) 1 else 0,
            anal = if (anal) 1 else 0,
            creampie = 0,
            analCreampie = 0,
            squirt = 0,
            facial = 0,
            firstEncounter = false,
            oral = if (oral) 1 else 0,
            gpIsGP = true,
            gpPrice = price
        )
    }

    private suspend fun addStandardPartners(pIdMap: MutableMap<String, Int>) {
        addP(pIdMap, "t1", "Tayne", 34, 0, "Badoo", "serious", "nearby", "11981951878", "Avenida Clara Rosa, 72, Carapicuíba", "single", "2026-04-19", "2021-09-16", "active", "crohn,desempregada")
        addP(pIdMap, "t2", "Juliete", 27, 2, "Badoo", "serious", "region", "11932024962", "295, Rua Coronel Silva Castro, São Paulo", "single", "2026-04-21", "2018-03-15", "active", "endometriose,esteril")
        addP(pIdMap, "t3", "Thays", 24, 0, "Badoo", "casual", "samestate", "11965682202", "1100, Avenida Paulo Faccini, Guarulhos", "unavailable", "2026-04-21", "2019-09-20", "inactive", "pcd")
        addP(pIdMap, "t4", "Letícia", 22, 1, "Lovoo", "serious", "region", "", "Estrada velha de Itapecerica, 240, São Paulo", "unavailable", "2026-04-21", "2015-04-21", "inactive", "fumante,drogas,mental,feminista,ativista_lgbt,instavel,ex_gp,rodada")
        addP(pIdMap, "t5", "Najara", 27, 0, "Jaumo", "serious", "nearby", "", "Rua São Philipe, Carapicuíba", "married", "2026-04-21", "2016-07-17", "inactive", "instavel")
        addP(pIdMap, "t6", "Maria Helena", 18, 1, "Jaumo", "casual", "region", "11959462925", "3, Rua Carlos Jordão Morales, Osasco", "unavailable", "2026-04-21", "2016-04-16", "inactive", "drogas,baladeira")
        addP(pIdMap, "t7", "Vanderléia", 31, 3, "Badoo", "casual", "samestate", "11991310826", "Rua Aimoré-Pixuma, 132, São Paulo", "lostcontact", "2026-04-21", "2017-01-15", "inactive", "bebebora")
        addP(pIdMap, "t8", "Lucilene", 33, 0, "Jaumo", "serious", "nearby", "", "Rua Antônio Teixeira Lacerda, 321, Carapicuíba", "lostcontact", "2026-04-21", "2017-12-29", "inactive", "")
        addP(pIdMap, "t9", "Raquel", 24, 0, "Jaumo", "serious", "nearby", "", "Estação Santa Terezinha, Carapicuíba", "lostcontact", "2026-04-21", "2017-03-23", "inactive", "")
        addP(pIdMap, "t10", "Mariana", 15, 0, "Pessoalmente", "casual", "region", "", "Cotia", "", "2026-04-23", "2011-11-01", "inactive", "")
        addP(pIdMap, "t11", "Dayane", 23, 2, "Badoo", "serious", "samestate", "", "Rua Hortências, 68, Itapecerica da Serra", "lostcontact", "2026-04-21", "2018-01-29", "inactive", "")
        addP(pIdMap, "t12", "Clara", 29, 0, "Jaumo", "serious", "nearby", "7198678732", "Rua Lutécia, 33, Carapicuíba", "farloc", "2026-04-21", "2018-03-19", "inactive", "")
        addP(pIdMap, "t13", "Lorana", 18, 0, "Badoo", "casual", "nearby", "11959588895", "281, estrada da fazendinha, Carapicuíba", "unavailable", "2026-04-21", "2020-06-29", "inactive", "")
        addP(pIdMap, "t14", "Nanda", 23, 1, "Badoo", "casual", "nearby", "", "Plaza Shopping Carapicuíba, Carapicuíba", "lostcontact", "2026-04-21", "2019-01-12", "inactive", "")
        addP(pIdMap, "t15", "Dayane Vargas", 26, 0, "Jaumo", "serious", "region", "11958611165", "Rua Vitório Tafarello, Osasco", "lostcontact", "2026-04-21", "2025-02-26", "inactive", "std,mental")
        addP(pIdMap, "t16", "Franceli", 22, 1, "Lovoo", "serious", "region", "", "Rua Alta Floresta, 475, Embu", "lostcontact", "2026-04-21", "2017-07-30", "inactive", "")
        addP(pIdMap, "t17", "Isabel", 24, 3, "Pessoalmente", "serious", "samestate", "", "Vila das belezas, São Paulo", "married", "2026-04-21", "2015-07-11", "inactive", "fumante")
        addP(pIdMap, "t18", "Rubia", 42, 2, "Jaumo", "serious", "nearby", "11948025155", "Rua Jorge zanardo, 260, Carapicuíba", "single", "2026-04-21", "2021-01-21", "inactive", "")
        addP(pIdMap, "t19", "Taís", 15, 0, "Pessoalmente", "casual", "region", "", "Cotia", "lostcontact", "2026-04-23", "2011-10-01", "inactive", "")
        addP(pIdMap, "t20", "Thainara", 22, 1, "Jaumo", "casual", "region", "", "Munhoz Júnior, Osasco", "lostcontact", "2026-04-21", "2017-05-27", "inactive", "")
        addP(pIdMap, "t21", "Milena", 26, 1, "Happn", "casual", "samestate", "", "Rua Tijuape, Morro do Índio, São Paulo", "farloc", "2026-04-21", "2018-03-11", "inactive", "")
        addP(pIdMap, "t22", "Aline", 28, 1, "Badoo", "casual", "", "11982907441", "230, Rua Antônio Borges, São Paulo", "lostcontact", "2026-04-21", "2019-08-07", "inactive", "")
        addP(pIdMap, "t23", "Michele Flores", 31, 2, "Lovoo", "casual", "nearby", "", "263, Avenida General Teixeira Lott, Carapicuíba", "married", "2026-04-21", "2019-02-04", "inactive", "")
        addP(pIdMap, "t24", "Leidiane", 33, 2, "Badoo", "casual", "samestate", "", "Vila Nova Cachoeirinha, São Paulo", "lostcontact", "2026-04-21", "2017-08-30", "inactive", "")
        
        addP(pIdMap, "t25", "Brenda", 22, 0, "Jaumo", "serious", "region", "", "Embu", "lostcontact", "2026-04-22", "2015-08-07", "inactive", "obesa")
        addP(pIdMap, "t26", "Dionete", 28, 1, "Facebook Namoro", "casual", "region", "", "1289, rua da Órbita, Santana de Parnaíba", "lostcontact", "2026-04-21", "2019-12-05", "inactive", "")
        addP(pIdMap, "t27", "Queila", 29, 3, "Lovoo", "casual", "samestate", "11999863201", "Rio Grande da Serra", "lostcontact", "2026-04-21", "2020-12-19", "inactive", "")
        addP(pIdMap, "t28", "Dianinha", 31, 0, "Happn", "casual", "region", "", "230, Rua Doutor Fomm, São Paulo", "lostcontact", "2026-04-21", "2018-06-09", "inactive", "instavel")
        addP(pIdMap, "t29", "Sabrina", 20, 1, "Pessoalmente", "casual", "region", "", "Barueri", "married", "2026-04-23", "2009-04-26", "inactive", "obesa")
        addP(pIdMap, "t30", "Deise", 27, 1, "Jaumo", "casual", "region", "", "45, Rua Pedro Rodrigues, Cotia", "lostcontact", "2026-04-21", "2018-07-30", "inactive", "instavel")
        addP(pIdMap, "t31", "Lucilene", 33, 1, "Jaumo", "casual", "region", "", "Osasco", "unavailable", "2026-04-23", "2017-12-29", "inactive", "ex_gp")
        addP(pIdMap, "t32", "Netinha", 40, 1, "Badoo", "casual", "region", "11998329220", "483, Caminho Furquim, Cotia", "lostcontact", "2026-04-21", "2021-05-03", "inactive", "pcd")
        addP(pIdMap, "t33", "Binha", 18, 1, "Pessoalmente", "serious", "region", "", "Osasco", "married", "2026-04-23", "2008-11-29", "inactive", "obesa,pcd")
        addP(pIdMap, "t34", "Tatiane", 34, 3, "Pessoalmente", "casual", "region", "", "Barueri", "married", "2026-04-23", "2018-01-15", "inactive", "")
        addP(pIdMap, "t35", "Cristiane", 36, 2, "Jaumo", "serious", "region", "", "Jandira", "lostcontact", "2026-04-21", "2016-09-01", "inactive", "")
        addP(pIdMap, "t36", "Katia", 33, 2, "Jaumo", "casual", "nearby", "", "31, Estrada Douglas Washington Gomes de Araújo, Carapicuíba", "lostcontact", "2026-04-21", "2019-01-25", "inactive", "instavel")
        addP(pIdMap, "t37", "Paula", 24, 1, "Pessoalmente", "casual", "nearby", "", "Carapicuíba", "lostcontact", "2026-04-23", "2011-09-27", "inactive", "bebebora,baladeira,rodada")
        addP(pIdMap, "t38", "Fran", 21, 1, "Pessoalmente", "casual", "region", "11984704315", "Osasco", "lostcontact", "2026-04-23", "2013-04-26", "inactive", "")
        addP(pIdMap, "t39", "Priscila", 21, 0, "Pessoalmente", "serious", "otherstate", "", "Santana de Cataguases", "married", "2026-04-23", "2012-02-25", "inactive", "instavel")
        addP(pIdMap, "t40", "Jaqueline", 24, 0, "Pessoalmente", "casual", "region", "", "Osasco", "lostcontact", "2026-04-23", "2011-12-06", "inactive", "")
        addP(pIdMap, "t41", "Letícia Papale", 17, 1, "Badoo", "casual", "region", "", "323, Orlando Alves Oliveira, Osasco", "lostcontact", "2026-04-21", "2019-01-25", "inactive", "rodada")
        addP(pIdMap, "t42", "Jéssica Lima", 19, 1, "Pessoalmente", "casual", "samestate", "", "Suzano", "lostcontact", "2026-04-23", "2013-06-05", "inactive", "")
        addP(pIdMap, "t43", "Beatriz", 16, 1, "Pessoalmente", "casual", "otherstate", "", "Santana de Cataguases", "married", "2026-04-23", "2012-08-15", "inactive", "")
        addP(pIdMap, "t44", "Denira", 28, 3, "Pessoalmente", "casual", "otherstate", "", "Santana de Cataguases", "farloc", "2026-04-23", "2014-01-04", "inactive", "fumante,bebebora")
        addP(pIdMap, "t45", "Mayara", 18, 1, "Pessoalmente", "casual", "nearby", "", "Carapicuíba", "lostcontact", "2026-04-23", "2015-08-20", "inactive", "fumante,rodada")
        addP(pIdMap, "t46", "Nelba", 24, 1, "Pessoalmente", "casual", "region", "11999041989", "Cotia", "married", "2026-04-23", "2013-04-25", "inactive", "fumante")
        addP(pIdMap, "t47", "Stefany Aoyama", 24, 1, "Jaumo", "casual", "region", "", "Osasco", "unavailable", "2026-04-21", "2015-10-16", "inactive", "fumante,drogas,bebebora,instavel,ex_gp,rodada")
        addP(pIdMap, "t48", "Tâmelis", 30, 4, "Pessoalmente", "serious", "region", "11943655418", "Rua Felipe Camarão, 139, Jandira", "married", "2026-04-21", "2016-09-09", "inactive", "fumante")
    }

    private suspend fun addGPs(pIdMap: MutableMap<String, Int>) {
        addGP(pIdMap, "gp1", "Japonesa", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp2", "Samara", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp3", "Sarita", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp4", "Luna", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp5", "Bia", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp6", "Paula", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp7", "Camila", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp8", "Camili", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp9", "Janaina", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp10", "Lara", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp11", "Vivi Loira", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp12", "Vanessa", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp13", "Loira Balzaca", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp14", "Rafaela Negra", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp15", "Bruna Loira", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp16", "Morena Namoradinha", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp17", "Nicole Loira", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp18", "Suelen", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp19", "Patrícia", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp20", "Andreza", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp21", "Nicole", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp22", "Novinha Morena", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp23", "Fernanda Tatoo MSP", "Andradas 69", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp24", "1 loira de sainha", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp25", "Loira Namoradinha", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp26", "2 de uma vez", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp27", "Loira Grande", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp28", "Ruiva Grande", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp29", "Tia Velha", "Andradas 69", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp30", "Negona", "Andradas 69", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp31", "Tia Loira", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp32", "Alana Magrinha", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp33", "Morena tatoo escorpião", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp34", "Ruiva Tatoo 🔥 na Xana", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp35", "Morena Índia", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp36", "Nega Magrinha do Anal", "Andradas 69", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp37", "Morena Sábado", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp38", "Cláudia", "Andradas 69", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp39", "Renata", "Andradas 69", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp40", "Flávia Loira Gata", "Andradas 69", "São Paulo", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp41", "Juliana Loira", "Andradas 69", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp42", "Nicole Loira", "Andradas 69", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp43", "Camila", "Andradas 69", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp44", "Marry", "Andradas 69", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp45", "Kelly", "Andradas 69", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp46", "Amanda", "Andradas 69", "São Paulo", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp47", "Morena Baixinha", "Hotel Brilhante", "Belo Horizonte", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp48", "Loira Novinha", "Hotel Brilhante", "Belo Horizonte", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp49", "Negona 33 anos", "Hotel Cabaré", "Belo Horizonte", "", "", "2026-04-23", 20.0)
        addGP(pIdMap, "gp50", "Morena Dançarina", "Hotel Cabaré", "Belo Horizonte", "", "", "2026-04-23", 20.0)
        addGP(pIdMap, "gp51", "Morena Estúpida", "Hotel Cabaré", "Belo Horizonte", "", "", "2026-04-23", 20.0)
        addGP(pIdMap, "gp52", "Morena Baixinha Magrinha", "Hotel Brilhante", "Belo Horizonte", "", "", "2026-04-23", 30.0)
        addGP(pIdMap, "gp53", "Gorda dos 3 manos", "Hotel Cine BH", "Belo Horizonte", "", "", "2026-04-23", 20.0)
        addGP(pIdMap, "gp54", "Morena Assada", "Bordel Taguatinga", "Taguatinga", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp55", "Doida Anal Campinas", "Jardim Itatinga", "Campinas", "", "", "2026-04-23", 50.0)
        addGP(pIdMap, "gp56", "Nordestina", "Anhangabaú", "São Paulo", "", "", "2026-04-23", 40.0)
        addGP(pIdMap, "gp57", "Morena praça Paysandu", "Anhangabaú", "São Paulo", "", "", "2026-04-23", 40.0)
        addGP(pIdMap, "gp58", "Morena Av. Primitiva", "Hotel Real Park Osasco", "Osasco", "", "", "2026-04-23", 40.0)
        addGP(pIdMap, "gp59", "Suzy", "Jardim Itatinga", "Campinas", "", "", "2026-04-23", 40.0)
        addGP(pIdMap, "gp60", "Bianca", "Prédio Barão de Limeira", "São Paulo", "", "", "2026-04-23", 40.0)
        addGP(pIdMap, "gp61", "Ana Vitória", "Conselheiro Furtado 151", "São Paulo", "", "", "2026-04-23", 50.0)
    }

    private suspend fun addStandardEncounters(pIdMap: MutableMap<String, Int>) {
        val datesTayne = listOf(
            "2026-03-24", "2026-02-10", "2025-12-16", "2025-09-09", "2025-06-15",
            "2025-04-17", "2025-03-20", "2024-12-18", "2024-11-26", "2024-10-09",
            "2024-08-28", "2024-06-06", "2024-03-09", "2023-11-29", "2023-06-07",
            "2022-11-23", "2022-08-15", "2022-07-14", "2022-05-26", "2022-04-26",
            "2022-04-10", "2022-02-12", "2022-01-13", "2021-12-28", "2021-12-02",
            "2021-10-27", "2021-09-16"
        )
        datesTayne.forEach { d ->
            val pId = pIdMap["t1"] ?: return@forEach
            val r = when(d) {
                "2026-03-24", "2026-02-10", "2024-03-09", "2022-04-26" -> 10.0
                "2021-10-27", "2021-09-16" -> 8.0
                else -> 9.0
            }
            val an = if (d == "2021-09-16") 0 else if (d == "2022-04-26") 2 else 1
            val cr = if (d == "2021-09-16") 0 else if (d == "2022-04-26") 2 else 1
            val sq = if (d == "2021-09-16") 0 else 1
            val fa = if (d == "2024-03-09" || d == "2021-09-16") 1 else 0
            val isFirst = d == "2021-09-16"
            addEncontro(pId, "Tayne", d, hadSex = true, rating = r, vaginal = 1, anal = an, creampie = cr, analCreampie = cr, squirt = sq, facial = fa, firstEncounter = isFirst)
        }

        val datesJuliete = listOf(
            "2026-05-08", "2026-04-21", "2018-08-07", "2018-04-20", "2018-04-15",
            "2018-04-13", "2018-04-10", "2018-04-03", "2018-03-28", "2018-03-15"
        )
        datesJuliete.forEach { d ->
            val pId = pIdMap["t2"] ?: return@forEach
            val r = when(d) {
                "2018-04-10" -> 10.0
                "2018-03-15", "2026-05-08", "2026-04-21" -> 8.0
                else -> 9.0
            }
            val hasAnal = !listOf("2026-05-08", "2026-04-21").contains(d)
            val an = if (hasAnal) 1 else 0
            val cr = if (hasAnal) 1 else 0
            val sq = if (d != "2018-04-20") 1 else 0
            val fa = if (d == "2018-04-20") 1 else 0
            addEncontro(pId, "Juliete", d, hadSex = true, rating = r, vaginal = 1, anal = an, creampie = cr, analCreampie = cr, squirt = sq, facial = fa, firstEncounter = (d == "2018-03-15"))
        }

        val pId_t3 = pIdMap["t3"] ?: 0
        if (pId_t3 > 0) {
            addEncontro(pId_t3, "Thays", "2019-10-22", hadSex = true, rating = 10.0, vaginal = 1, anal = 2, creampie = 2, analCreampie = 2, squirt = 1)
            addEncontro(pId_t3, "Thays", "2019-10-04", hadSex = true, rating = 10.0, vaginal = 1, anal = 2, creampie = 2, analCreampie = 2, squirt = 2)
            addEncontro(pId_t3, "Thays", "2019-09-20", hadSex = true, rating = 10.0, firstEncounter = true, vaginal = 1, anal = 2, creampie = 2, analCreampie = 2, squirt = 2)
        }

        val pId_t4 = pIdMap["t4"] ?: 0
        if (pId_t4 > 0) {
            addEncontro(pId_t4, "Letícia", "2016-09-25", hadSex = true, rating = 10.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t4, "Letícia", "2016-09-21", hadSex = true, rating = 9.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t4, "Letícia", "2016-01-10", hadSex = true, rating = 9.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t4, "Letícia", "2015-12-29", hadSex = true, rating = 8.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t4, "Letícia", "2015-12-13", hadSex = true, rating = 8.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t4, "Letícia", "2015-11-02", hadSex = true, rating = 7.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
            addEncontro(pId_t4, "Letícia", "2015-10-12", hadSex = true, rating = 10.0, vaginal = 1, anal = 3, creampie = 3, analCreampie = 3, squirt = 3)
            addEncontro(pId_t4, "Letícia", "2015-10-02", hadSex = false, rating = 0.0)
            addEncontro(pId_t4, "Letícia", "2015-04-21", hadSex = true, rating = 8.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
        }

        val pId_t5 = pIdMap["t5"] ?: 0
        if (pId_t5 > 0) {
            addEncontro(pId_t5, "Najara", "2016-08-04", hadSex = true, rating = 9.0, vaginal = 1, anal = 2, creampie = 2, analCreampie = 2)
            addEncontro(pId_t5, "Najara", "2016-07-24", hadSex = true, rating = 9.0, vaginal = 1, anal = 2, creampie = 2, analCreampie = 2)
            addEncontro(pId_t5, "Najara", "2016-07-17", hadSex = true, rating = 9.0, vaginal = 1, anal = 2, creampie = 2, analCreampie = 2)
        }

        val pId_t6 = pIdMap["t6"] ?: 0
        if (pId_t6 > 0) {
            addEncontro(pId_t6, "Maria Helena", "2017-11-30", hadSex = true, rating = 8.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t6, "Maria Helena", "2017-04-21", hadSex = true, rating = 9.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t6, "Maria Helena", "2016-04-16", hadSex = true, rating = 9.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
        }

        val pId_t7 = pIdMap["t7"] ?: 0
        if (pId_t7 > 0) {
            addEncontro(pId_t7, "Vanderléia", "2018-03-21", hadSex = true, rating = 9.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t7, "Vanderléia", "2018-03-06", hadSex = true, rating = 8.0, vaginal = 1)
            addEncontro(pId_t7, "Vanderléia", "2018-02-14", hadSex = true, rating = 10.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t7, "Vanderléia", "2018-01-23", hadSex = true, rating = 9.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t7, "Vanderléia", "2017-01-15", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, squirt = 1)
        }

        val pId_t8 = pIdMap["t8"] ?: 0
        if (pId_t8 > 0) {
            addEncontro(pId_t8, "Lucilene", "2018-03-30", hadSex = true, rating = 9.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t8, "Lucilene", "2018-03-08", hadSex = true, rating = 9.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
            addEncontro(pId_t8, "Lucilene", "2018-01-21", hadSex = true, rating = 9.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t8, "Lucilene", "2017-12-31", hadSex = true, rating = 8.0, vaginal = 1, squirt = 1)
            addEncontro(pId_t8, "Lucilene", "2017-12-29", hadSex = false, rating = 0.0)
        }

        val pId_t9 = pIdMap["t9"] ?: 0
        if (pId_t9 > 0) {
            addEncontro(pId_t9, "Raquel", "2017-03-30", hadSex = true, rating = 7.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
            addEncontro(pId_t9, "Raquel", "2017-03-23", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
        }

        val pId_t10 = pIdMap["t10"] ?: 0
        if (pId_t10 > 0) {
            addEncontro(pId_t10, "Mariana", "2011-11-01", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, squirt = 1)
        }
        val pId_t11 = pIdMap["t11"] ?: 0
        if (pId_t11 > 0) {
            addEncontro(pId_t11, "Dayane", "2018-01-29", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, anal = 4, creampie = 4, analCreampie = 4, squirt = 1)
        }
        val pId_t12 = pIdMap["t12"] ?: 0
        if (pId_t12 > 0) {
            addEncontro(pId_t12, "Clara", "2018-03-26", hadSex = true, rating = 9.0, vaginal = 1, squirt = 1)
            addEncontro(pId_t12, "Clara", "2018-03-24", hadSex = true, rating = 9.0, vaginal = 1, squirt = 1)
            addEncontro(pId_t12, "Clara", "2018-03-22", hadSex = true, rating = 9.0, vaginal = 1, squirt = 1)
            addEncontro(pId_t12, "Clara", "2018-03-20", hadSex = true, rating = 9.0, vaginal = 1, squirt = 1)
            addEncontro(pId_t12, "Clara", "2018-03-19", hadSex = false, rating = 0.0)
            addEncontro(pId_t12, "Clara", "2018-03-24", hadSex = true, rating = 9.0, vaginal = 1)
        }
        val pId_t13 = pIdMap["t13"] ?: 0
        if (pId_t13 > 0) {
            addEncontro(pId_t13, "Lorana", "2020-06-29", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
        }
        val pId_t14 = pIdMap["t14"] ?: 0
        if (pId_t14 > 0) {
            addEncontro(pId_t14, "Nanda", "2019-01-12", hadSex = true, rating = 7.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
        }
        val pId_t15 = pIdMap["t15"] ?: 0
        if (pId_t15 > 0) {
            addEncontro(pId_t15, "Dayane Vargas", "2025-02-26", hadSex = true, rating = 8.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
        }

        val pId_t16 = pIdMap["t16"] ?: 0
        if (pId_t16 > 0) {
            addEncontro(pId_t16, "Franceli", "2018-06-10", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, squirt = 1)
            addEncontro(pId_t16, "Franceli", "2017-07-30", hadSex = true, rating = 9.0, vaginal = 1, squirt = 1)
        }

        val pId_t17 = pIdMap["t17"] ?: 0
        if (pId_t17 > 0) {
            addEncontro(pId_t17, "Isabel", "2015-09-12", hadSex = true, rating = 9.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
            addEncontro(pId_t17, "Isabel", "2015-08-09", hadSex = true, rating = 9.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
            addEncontro(pId_t17, "Isabel", "2015-07-24", hadSex = true, rating = 0.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
            addEncontro(pId_t17, "Isabel", "2015-07-11", hadSex = true, rating = 8.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
        }

        val pId_t18 = pIdMap["t18"] ?: 0
        if (pId_t18 > 0) {
            addEncontro(pId_t18, "Rubia", "2021-01-21", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
        }
        val pId_t19 = pIdMap["t19"] ?: 0
        if (pId_t19 > 0) {
            addEncontro(pId_t19, "Taís", "2011-10-01", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1)
        }
        val pId_t20 = pIdMap["t20"] ?: 0
        if (pId_t20 > 0) {
            addEncontro(pId_t20, "Thainara", "2017-05-27", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
        }

        val pId_t21 = pIdMap["t21"] ?: 0
        if (pId_t21 > 0) {
            addEncontro(pId_t21, "Milena", "2021-04-18", hadSex = true, rating = 7.0, vaginal = 1)
            addEncontro(pId_t21, "Milena", "2018-03-11", hadSex = true, rating = 8.0, firstEncounter = true, vaginal = 1)
        }
        val pId_t22 = pIdMap["t22"] ?: 0
        if (pId_t22 > 0) {
            addEncontro(pId_t22, "Aline", "2019-08-07", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
        }
        val pId_t23 = pIdMap["t23"] ?: 0
        if (pId_t23 > 0) {
            addEncontro(pId_t23, "Michele Flores", "2019-02-04", hadSex = true, rating = 6.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
        }
        val pId_t24 = pIdMap["t24"] ?: 0
        if (pId_t24 > 0) {
            addEncontro(pId_t24, "Leidiane", "2017-08-30", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, anal = 2, creampie = 2, analCreampie = 2, squirt = 1)
        }

        val pId_t25 = pIdMap["t25"] ?: 0
        if (pId_t25 > 0) {
            addEncontro(pId_t25, "Brenda", "2015-08-29", hadSex = true, rating = 6.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
            addEncontro(pId_t25, "Brenda", "2015-08-19", hadSex = true, rating = 6.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
            addEncontro(pId_t25, "Brenda", "2015-08-07", hadSex = false, rating = 0.0)
        }
        val pId_t26 = pIdMap["t26"] ?: 0
        if (pId_t26 > 0) {
            addEncontro(pId_t26, "Dionete", "2019-12-05", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, squirt = 1)
        }
        val pId_t27 = pIdMap["t27"] ?: 0
        if (pId_t27 > 0) {
            addEncontro(pId_t27, "Queila", "2020-12-19", hadSex = true, rating = 8.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
        }
        val pId_t28 = pIdMap["t28"] ?: 0
        if (pId_t28 > 0) {
            addEncontro(pId_t28, "Dianinha", "2018-06-09", hadSex = true, rating = 2.0, firstEncounter = true, vaginal = 1)
        }

        val pId_t29 = pIdMap["t29"] ?: 0
        if (pId_t29 > 0) {
            addEncontro(pId_t29, "Sabrina", "2009-04-26", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
        }
        val pId_t30 = pIdMap["t30"] ?: 0
        if (pId_t30 > 0) {
            addEncontro(pId_t30, "Deise", "2018-07-30", hadSex = true, rating = 2.0, firstEncounter = true, vaginal = 1)
        }
        val pId_t31 = pIdMap["t31"] ?: 0
        if (pId_t31 > 0) {
            addEncontro(pId_t31, "Lucilene", "2017-12-29", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
        }
        val pId_t32 = pIdMap["t32"] ?: 0
        if (pId_t32 > 0) {
            addEncontro(pId_t32, "Netinha", "2021-05-03", hadSex = true, rating = 4.0, firstEncounter = true, vaginal = 1)
        }
        val pId_t33 = pIdMap["t33"] ?: 0
        if (pId_t33 > 0) {
            addEncontro(pId_t33, "Binha", "2008-11-29", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, isVirginityLost = true)
        }
        val pId_t34 = pIdMap["t34"] ?: 0
        if (pId_t34 > 0) {
            addEncontro(pId_t34, "Tatiane", "2018-01-15", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
        }
        val pId_t35 = pIdMap["t35"] ?: 0
        if (pId_t35 > 0) {
            addEncontro(pId_t35, "Cristiane", "2016-09-01", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, squirt = 1)
        }
        val pId_t36 = pIdMap["t36"] ?: 0
        if (pId_t36 > 0) {
            addEncontro(pId_t36, "Katia", "2019-01-25", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, squirt = 1)
        }
        val pId_t37 = pIdMap["t37"] ?: 0
        if (pId_t37 > 0) {
            addEncontro(pId_t37, "Paula", "2011-09-27", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1)
        }
        val pId_t38 = pIdMap["t38"] ?: 0
        if (pId_t38 > 0) {
            addEncontro(pId_t38, "Fran", "2013-04-26", hadSex = true, rating = 0.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1, squirt = 1)
        }
        val pId_t39 = pIdMap["t39"] ?: 0
        if (pId_t39 > 0) {
            addEncontro(pId_t39, "Priscila", "2012-02-25", hadSex = true, rating = 0.0, vaginal = 1, anal = 1, creampie = 1, analCreampie = 1)
        }
        val pId_t40 = pIdMap["t40"] ?: 0
        if (pId_t40 > 0) {
            addEncontro(pId_t40, "Jaqueline", "2011-12-06", hadSex = true, rating = 0.0, vaginal = 1)
        }
        val pId_t41 = pIdMap["t41"] ?: 0
        if (pId_t41 > 0) {
            addEncontro(pId_t41, "Letícia Papale", "2019-02-11", hadSex = true, rating = 2.0, vaginal = 1)
            addEncontro(pId_t41, "Letícia Papale", "2019-01-25", hadSex = false, rating = 0.0)
        }
        val pId_t42 = pIdMap["t42"] ?: 0
        if (pId_t42 > 0) {
            addEncontro(pId_t42, "Jéssica Lima", "2013-06-05", hadSex = true, rating = 0.0, vaginal = 1)
        }
        val pId_t43 = pIdMap["t43"] ?: 0
        if (pId_t43 > 0) {
            addEncontro(pId_t43, "Beatriz", "2012-08-15", hadSex = true, rating = 0.0, vaginal = 1)
        }
        val pId_t44 = pIdMap["t44"] ?: 0
        if (pId_t44 > 0) {
            addEncontro(pId_t44, "Denira", "2014-01-04", hadSex = true, rating = 0.0, firstEncounter = true, vaginal = 1, squirt = 1)
        }
        val pId_t45 = pIdMap["t45"] ?: 0
        if (pId_t45 > 0) {
            addEncontro(pId_t45, "Mayara", "2015-08-20", hadSex = true, rating = 0.0, vaginal = 1)
        }
        val pId_t46 = pIdMap["t46"] ?: 0
        if (pId_t46 > 0) {
            addEncontro(pId_t46, "Nelba", "2013-04-25", hadSex = true, rating = 0.0, vaginal = 1, squirt = 1)
        }
        val pId_t47 = pIdMap["t47"] ?: 0
        if (pId_t47 > 0) {
            addEncontro(pId_t47, "Stefany Aoyama", "2015-10-16", hadSex = true, rating = 7.0, firstEncounter = true, vaginal = 1, squirt = 1)
        }
        val pId_t48 = pIdMap["t48"] ?: 0
        if (pId_t48 > 0) {
            addEncontro(pId_t48, "Tâmelis", "2016-09-09", hadSex = true, rating = 6.0, vaginal = 1, squirt = 1)
        }
    }

    private suspend fun addGPEncounters(pIdMap: MutableMap<String, Int>) {
        addGPEncounter(pIdMap, "gp1", "2013-01-12", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp2", "2013-01-26", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp3", "2013-02-02", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp4", "2013-02-16", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp5", "2013-03-02", vaginal = true, anal = false, oral = false, price = 30.0)
        
        addGPEncounter(pIdMap, "gp6", "2013-03-16", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp6", "2013-03-23", vaginal = true, anal = false, oral = false, price = 30.0)
        
        addGPEncounter(pIdMap, "gp7", "2013-03-30", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp8", "2013-04-06", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp9", "2013-04-13", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp10", "2013-04-20", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp11", "2013-04-27", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp12", "2013-05-04", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp13", "2013-05-11", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp14", "2013-05-18", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp15", "2013-05-25", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp16", "2013-06-01", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp17", "2013-06-08", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp18", "2013-06-15", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp19", "2013-06-22", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp20", "2013-06-29", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp21", "2013-07-06", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp22", "2008-07-13", vaginal = true, anal = false, oral = false, price = 30.0)
        
        addGPEncounter(pIdMap, "gp23", "2009-07-20", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp23", "2008-07-27", vaginal = true, anal = true, oral = false, price = 50.0)
        
        addGPEncounter(pIdMap, "gp24", "2005-09-10", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp25", "2008-08-03", vaginal = true, anal = false, oral = false, price = 30.0)
        
        addGPEncounter(pIdMap, "gp26", "2007-08-10", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp26", "2007-08-10", vaginal = true, anal = false, oral = false, price = 30.0)
        
        addGPEncounter(pIdMap, "gp27", "2007-08-17", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp28", "2006-08-24", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp29", "2008-08-31", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp30", "2006-09-07", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp31", "2006-09-14", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp32", "2007-09-21", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp33", "2007-09-28", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp34", "2007-10-05", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp35", "2007-10-12", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp36", "2006-10-19", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp37", "2006-10-28", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp38", "2007-11-02", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp39", "2006-11-09", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp40", "2007-11-16", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp41", "2006-11-23", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp42", "2005-11-30", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp43", "2005-12-07", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp44", "2005-12-14", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp45", "2005-12-21", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp46", "2005-12-28", vaginal = true, anal = true, oral = false, price = 50.0)
        
        addGPEncounter(pIdMap, "gp47", "2007-04-21", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp47", "2007-04-24", vaginal = true, anal = false, oral = false, price = 30.0)
        
        addGPEncounter(pIdMap, "gp48", "2007-04-28", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp49", "2007-05-05", vaginal = true, anal = false, oral = false, price = 20.0)
        addGPEncounter(pIdMap, "gp50", "2007-05-12", vaginal = true, anal = false, oral = false, price = 20.0)
        addGPEncounter(pIdMap, "gp51", "2007-05-19", vaginal = true, anal = false, oral = false, price = 20.0)
        addGPEncounter(pIdMap, "gp52", "2007-05-08", vaginal = true, anal = false, oral = false, price = 30.0)
        addGPEncounter(pIdMap, "gp53", "2007-05-28", vaginal = true, anal = false, oral = false, price = 20.0)
        addGPEncounter(pIdMap, "gp54", "2007-03-30", vaginal = true, anal = false, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp55", "2011-07-10", vaginal = true, anal = true, oral = false, price = 50.0)
        addGPEncounter(pIdMap, "gp56", "2005-07-11", vaginal = true, anal = false, oral = false, price = 40.0)
        addGPEncounter(pIdMap, "gp57", "2005-05-17", vaginal = true, anal = false, oral = false, price = 40.0)
        addGPEncounter(pIdMap, "gp58", "2005-07-13", vaginal = true, anal = true, oral = false, price = 40.0)
        
        addGPEncounter(pIdMap, "gp59", "2024-09-21", vaginal = true, anal = true, oral = true, price = 40.0)
        addGPEncounter(pIdMap, "gp59", "2023-12-15", vaginal = true, anal = false, oral = true, price = 40.0)
        addGPEncounter(pIdMap, "gp59", "2026-05-06", vaginal = true, anal = true, oral = true, price = 50.0)
        
        addGPEncounter(pIdMap, "gp60", "2023-12-30", vaginal = true, anal = false, oral = false, price = 40.0)
        addGPEncounter(pIdMap, "gp61", "2025-05-20", vaginal = true, anal = false, oral = false, price = 50.0)
    }
}
