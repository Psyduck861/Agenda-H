package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BadgeModel(
    val id: String,
    val icon: String,
    val text: String,
    val isNegative: Boolean = false,
    val isRelationship: Boolean = false,
    val isVolatile: Boolean = false,
    val color: String = ""
) {
    val type: String get() = id
    val label: String get() = text
}

object WomanCalculator {

    // Negative Traits Definition matching the spec
    data class NegativeTrait(
        val id: String,
        val label: String,
        val icon: String,
        val penalty: Int,
        val category: String
    )

    val negativeTraitsList = listOf(
        // Vícios
        NegativeTrait("fumante", "Fumante", "🚬", 15, "Vícios"),
        NegativeTrait("drogas", "Usuária de Drogas", "💊", 35, "Vícios"),
        NegativeTrait("bebebora", "Bebe Muito", "🍺", 15, "Vícios"),
        NegativeTrait("jogadora", "Vício em Jogo", "🎰", 20, "Vícios"),
        // Saúde
        NegativeTrait("obesa", "Obesa", "🍔", 20, "Saúde"),
        NegativeTrait("endometriose", "Endometriose", "🩺", 10, "Saúde"),
        NegativeTrait("crohn", "Crohn/Colite", "🩹", 10, "Saúde"),
        NegativeTrait("esteril", "Não pode ter filhos", "🚫", 5, "Saúde"),
        NegativeTrait("dst", "DST", "⚠️", 20, "Saúde"),
        NegativeTrait("mental", "Problema mental grave", "🧠", 20, "Saúde"),
        NegativeTrait("pcd", "PCD", "🧑‍🦽", 10, "Saúde"),
        // Comportamento
        NegativeTrait("feminista", "Feminista Militante", "📢", 15, "Comportamento"),
        NegativeTrait("ativista_lgbt", "Ativista LGBT", "🌈", 10, "Comportamento"),
        NegativeTrait("esquerdista", "Esquerdista Militante", "🚩", 10, "Comportamento"),
        NegativeTrait("vegana", "Vegana Militante", "🌱", 10, "Comportamento"),
        NegativeTrait("instavel", "Instável Emocional", "🌀", 20, "Comportamento"),
        NegativeTrait("baladeira", "Muito Baladeira", "🕺", 10, "Comportamento"),
        // Situação
        NegativeTrait("familia", "Família Desestruturada", "🏠", 15, "Situação"),
        NegativeTrait("foto", "Engano da Foto", "📸", 25, "Situação"),
        NegativeTrait("ex_gp", "Foi GP", "👠", 25, "Situação"),
        NegativeTrait("onlyfans", "Tem OnlyFans", "🔞", 20, "Situação"),
        NegativeTrait("desempregada", "Desempregada", "💼", 10, "Situação"),
        NegativeTrait("dividas", "Muitas Dívidas", "💸", 10, "Situação"),
        NegativeTrait("ex", "Ex Complicado", "🙅", 10, "Situação"),
        NegativeTrait("rodada", "Mulher Rodada", "🎡", 15, "Situação"),
        NegativeTrait("religiosa", "Extremamente Religiosa", "⛪", 10, "Situação")
    )

    fun extractYear(dateStr: String): Int {
        val trimmed = dateStr.trim()
        if (trimmed.isEmpty()) return Calendar.getInstance().get(Calendar.YEAR)
        
        // Match yyyy-MM-dd
        val partsHyphen = trimmed.split("-")
        if (partsHyphen.size >= 3 && partsHyphen[0].length == 4) {
            return partsHyphen[0].toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        }
        // Match dd/MM/yyyy
        val partsSlash = trimmed.split("/")
        if (partsSlash.size >= 3 && partsSlash[2].length == 4) {
            return partsSlash[2].toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        }
        
        // Find 4 digits
        val regex = Regex("\\d{4}")
        val match = regex.find(trimmed)
        if (match != null) {
            return match.value.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        }
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    // Spec 6 - Age calculations
    fun calculateCurrentAge(partner: Partner): Int {
        if (partner.firstDate.isEmpty() || partner.age <= 0) return partner.age
        val firstYear = extractYear(partner.firstDate)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val diff = currentYear - firstYear
        return partner.age + if (diff > 0) diff else 0
    }

    fun formatAbstinenceYMD(days: Int): String {
        if (days < 0) return "Sem sexo registrado"
        if (days == 0) return "Hoje"
        val years = days / 365
        val remMonths = days % 365
        val months = remMonths / 30
        val rDays = remMonths % 30
        
        val list = mutableListOf<String>()
        if (years > 0) {
            list.add(if (years == 1) "1 ano" else "$years anos")
        }
        if (months > 0) {
            list.add(if (months == 1) "1 mês" else "$months meses")
        }
        if (rDays > 0) {
            list.add(if (rDays == 1) "1 dia" else "$rDays dias")
        }
        
        if (list.isEmpty()) return "Hoje"
        if (list.size == 1) return list[0]
        if (list.size == 2) return "${list[0]} e ${list[1]}"
        return "${list[0]}, ${list[1]} e ${list[2]}"
    }

    fun calculateAgeAtEncounter(partner: Partner, encounterDate: String): Int {
        if (partner.firstDate.isEmpty() || partner.age <= 0) return partner.age
        val firstYear = extractYear(partner.firstDate)
        val encounterYear = extractYear(encounterDate)
        val diff = encounterYear - firstYear
        return partner.age + if (diff > 0) diff else 0
    }

    fun calculateDaysBetween(startDateStr: String, endDateStr: String): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfBr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        val parse = { str: String ->
            try {
                if (str.contains("-")) sdf.parse(str) else sdfBr.parse(str)
            } catch (e: Exception) {
                null
            }
        }

        val start = parse(startDateStr) ?: return 100
        val end = parse(endDateStr) ?: return 0
        val diffMs = end.time - start.time
        val diffDays = diffMs / (1000 * 60 * 60 * 24)
        return if (diffDays < 0) 0 else diffDays.toInt()
    }

    fun abstinencePenalty(days: Int): Int {
        return when {
            days <= 7 -> 0
            days <= 30 -> -15
            days <= 60 -> -30
            days <= 90 -> -50
            days <= 180 -> -80
            days <= 365 -> -120
            else -> -180
        }
    }

    fun isNPot(partner: Partner, encounters: List<Encontro>): Boolean {
        val createdAtStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(partner.createdAt))
        val daysSinceCreated = calculateDaysBetween(createdAtStr, getTodayDateString())
        val months = daysSinceCreated / 30.0
        val sexEncs = encounters.filter { it.hadSex }
        if (sexEncs.isEmpty() || months < 3.0) return false
        val cnt = sexEncs.size
        val lastSexD = sexEncs.maxByOrNull { it.date }?.date ?: return false
        val daysSex = calculateDaysBetween(lastSexD, getTodayDateString())
        
        val hasBigFreq = encounters.any { e ->
            e.typeAnal || e.typeCreampie || e.typeVirginity || e.typeFirstEncounter
        }
        return months >= 3.0 && cnt >= 3 && daysSex <= 60 && hasBigFreq
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // core Woman Score (scoreW) rules port of V9
    fun calculateScoreW(partner: Partner, encounters: List<Encontro>): Int {
        var s = 0 // V9 starts at s=0 baseline

        // 1. Age (Current Age) fertile window 18-28 best
        val age = calculateCurrentAge(partner)
        if (age in 18..22) s += 30
        else if (age in 23..28) s += 25
        else if (age in 29..32) s += 15
        else if (age in 33..36) s += 5
        else if (age > 36) s -= minOf(20, (age - 36) * 2)

        // 2. Children of her
        val ch = partner.children
        if (ch == 0) s += 15 else if (ch == 1) s -= 10 else s -= ch * 15

        // 3. Negatives list penalty
        val negIds = (partner.negatives ?: "").split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        for (nid in negIds) {
            val trait = negativeTraitsList.find { it.id == nid }
            if (trait != null) {
                s -= trait.penalty
            }
        }

        // 4. Objective
        // serious: 15, casual: 10, maybe: 5, friendship: -10, chat: -5, packs: -20
        val obj = (partner.objective ?: "casual").lowercase()
        s += when {
            obj.contains("sério") || obj.contains("serio") -> 15
            obj.contains("ficar") || obj.contains("casual") -> 10
            obj.contains("no que dá") || obj.contains("no que da") -> 5
            obj.contains("amizade") || obj.contains("conversar") -> -10
            obj.contains("packs") || obj.contains("fotos") -> -20
            else -> 0
        }

        // 5. Location
        // nearby: 10, region: 5, samestate: 0, otherstate: -10, abroad: 10
        val loc = (partner.location ?: "nearby").lowercase()
        s += when {
            loc.contains("<5km") || loc.contains("mesma cidade") || loc.contains("capital") -> 10
            loc.contains("próximo") || loc.contains("proximo") || loc.contains("5-20km") || loc.contains("região") -> 5
            loc.contains("mesmo estado") || loc.contains(">20km") -> 0
            loc.contains("outro estado") || loc.contains("longe") -> -10
            loc.contains("exterior") || loc.contains("viagem") || loc.contains("outro país") -> 10
            else -> 0
        }

        // 6. Availability bonus
        val isSingle = partner.availability.equals("Solteira", ignoreCase = true) || 
                partner.availability.isEmpty() || 
                partner.availability.equals("single", ignoreCase = true)
        if (isSingle) s += 20 else s -= 30

        // 7. Encounters & Sex details
        val sexEncs = encounters.filter { it.hadSex }
        val hs = sexEncs.isNotEmpty()
        if (!hs && encounters.isNotEmpty()) {
            s -= encounters.size * 5
        }
        
        sexEncs.forEach { e ->
            if (e.rating > 0) s += (e.rating * 2.0).toInt()
            if (e.typeVirginity) s += 100
            if (e.typeFirstEncounter) s += 80
            if (e.typeCreampie) s += 120
            if (e.typeAnalCreampie) s += 180
            if (e.typeAnal) s += 8
            if (e.typeOral) s += 3
            if (e.typeFacial) s += 5
            if (e.typeSquirt) s += 5
            if (e.typeDeepthroat) s += 3
            if (e.isPregnancy || e.isPregnancyMarked) s += 2
            if (e.typeVaginal) s += 2
        }

        // 8. Recency of sex & abstinence penalty
        if (hs) {
            val lastSex = sexEncs.maxByOrNull { it.date }
            val ds = if (lastSex != null) calculateDaysBetween(lastSex.date, getTodayDateString()) else 999
            if (ds <= 7) s += 30 else if (ds <= 30) s += 20 else if (ds <= 90) s += 10
            s += abstinencePenalty(ds)
        } else {
            val lastContactDate = encounters.maxByOrNull { it.date }?.date ?: partner.firstDate.ifEmpty { partner.createdAt.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } }
            val dsCont = calculateDaysBetween(lastContactDate, getTodayDateString())
            s += abstinencePenalty(maxOf(dsCont, 30))
        }

        // 9. Volume
        s += minOf(50, sexEncs.size * 5)
        if (isNPot(partner, encounters)) s += 20

        // 10. Paternity bonus / Pregnancy limits
        val myKids = maxOf(encounters.count { it.isPregnancy || it.isPregnancyMarked }, partner.myKids)
        s += myKids * 25

        val notesLower = partner.notes?.lowercase() ?: ""
        val isPregnant = notesLower.contains("grávida") || notesLower.contains("gravidez") || encounters.any { it.isPregnancy || it.isPregnancyMarked }
        if (isPregnant || myKids > 0) {
            return 999
        }

        return s
    }

    // Priority score = scoreW + heat bonus
    fun calculatePriorityScore(partner: Partner, encounters: List<Encontro>): Int {
        val base = calculateScoreW(partner, encounters)
        val sexEncs = encounters.filter { it.hadSex }
        val hs = sexEncs.isNotEmpty()
        val lastSex = if (hs) sexEncs.maxByOrNull { it.date }?.date else null
        val ds = if (lastSex != null) calculateDaysBetween(lastSex, getTodayDateString()) else 9999
        val freq = sexEncs.size
        
        val recBonus = when {
            ds <= 7 -> 20
            ds <= 30 -> 12
            ds <= 90 -> 5
            else -> 0
        }
        val freqBonus = minOf(15, freq * 2)
        return base + recBonus + freqBonus
    }

    // affPct: Math.max(0,Math.min(100,Math.round(((s+120)/420)*100)))
    fun calculateAffinityPercent(partner: Partner, encounters: List<Encontro>): Int {
        val s = calculateScoreW(partner, encounters)
        return maxOf(0, minOf(100, Math.round(((s + 120) / 420.0) * 100).toInt()))
    }

    fun calculateAffinityLabel(partner: Partner, encounters: List<Encontro>): String {
        val pct = calculateAffinityPercent(partner, encounters)
        return when {
            pct >= 85 -> "Altíssima"
            pct >= 70 -> "Muito Alta"
            pct >= 55 -> "Alta"
            pct >= 40 -> "Média"
            pct >= 25 -> "Baixa"
            else -> "Péssima"
        }
    }

    fun calculateHeatScore(partner: Partner, encounters: List<Encontro>): String {
        val sexEncs = encounters.filter { it.hadSex }
        if (sexEncs.isEmpty()) return "Fria"
        val last = sexEncs.maxByOrNull { it.date }?.date ?: return "Fria"
        val ds = calculateDaysBetween(last, getTodayDateString())
        return when {
            ds <= 14 -> "Quente"
            ds <= 60 -> "Morna"
            else -> "Fria"
        }
    }

    fun calculateBorderColorGroup(partner: Partner, encounters: List<Encontro>): String {
        val isInactive = partner.status.equals("Inativa", ignoreCase = true)
        if (isInactive) return "Cinza"
        
        val isSingle = partner.availability.equals("Solteira", ignoreCase = true) || 
                partner.availability.isEmpty() || 
                partner.availability.equals("single", ignoreCase = true)
        if (!isSingle) return "Cinza"
        
        val sexEncs = encounters.filter { it.hadSex }
        val hs = sexEncs.isNotEmpty()
        if (!hs && encounters.size >= 3) return "Cinza"
        
        if (hs) {
            val last = sexEncs.maxByOrNull { it.date }
            val ds = if (last != null) calculateDaysBetween(last.date, getTodayDateString()) else 999
            if (ds > 365) return "Cinza"
            
            val p = calculateAffinityPercent(partner, encounters)
            return when {
                p >= 65 -> "Ouro"
                p >= 40 -> "Prata"
                else -> "Bronze"
            }
        } else {
            val lastContactDate = encounters.maxByOrNull { it.date }?.date ?: partner.firstDate.ifEmpty { partner.createdAt.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } }
            val d = calculateDaysBetween(lastContactDate, getTodayDateString())
            return when {
                d <= 7 -> "Verde"
                d <= 21 -> "Amarelo"
                else -> "Vermelho"
            }
        }
    }

    fun calculateBadges(partner: Partner, encounters: List<Encontro>, isMostRecentSexGlobal: Boolean): List<BadgeModel> {
        val badges = mutableListOf<BadgeModel>()
        
        fun addUniqueBadge(id: String, icon: String, text: String, isNegative: Boolean = false, isRelationship: Boolean = false, isVolatile: Boolean = false, color: String = "") {
            if (id.isNotEmpty() && icon.isNotEmpty() && text.isNotEmpty()) {
                if (badges.none { it.id == id }) {
                    badges.add(BadgeModel(id, icon, text, isNegative, isRelationship, isVolatile, color))
                }
            }
        }
        
        val sexEncs = encounters.filter { it.hadSex }
        
        // UNIQUE EVENTS
        // 1. Virginity
        val explicitlyLostVirginity = encounters.any { it.typeVirginity || it.isVirginityLost }
        val notesSafeForVirgin = partner.notes?.lowercase() ?: ""
        val notedAsVirgin = notesSafeForVirgin.contains("virgem") || 
                notesSafeForVirgin.contains("virginidade")
        if (explicitlyLostVirginity || (notedAsVirgin && partner.children == 0 && partner.myKids == 0)) {
            addUniqueBadge("virginity", "🎀", "Perda da Virgindade")
        }
        
        // 2. First encounter sex
        val sortedAsc = encounters.sortedBy { it.date }
        if (sortedAsc.isNotEmpty()) {
            val first = sortedAsc.first()
            if (first.hadSex && (first.typeVaginal || first.typeAnal || first.typeOral || first.typeFirstEncounter || first.isFirstEncontroSex)) {
                addUniqueBadge("firstencounter", "🏁", "Sexo no 1º Encontro")
            }
        }
        
        // PROGRESSIVE FETISHES
        val vaginalCount = encounters.count { it.typeVaginal }
        val analCount = encounters.count { it.typeAnal }
        val creampieCount = encounters.sumOf { if (it.typeCreampie) maxOf(1, it.countCreampie) else 0 }
        val analCreampieCount = encounters.sumOf { if (it.typeAnalCreampie) maxOf(1, it.countAnalCreampie) else 0 }
        val oralCount = encounters.count { it.typeOral }
        val facialCount = encounters.count { it.typeFacial }
        val squirtCount = encounters.count { it.typeSquirt }
        val deepthroatCount = encounters.count { it.typeDeepthroat }
        val pregnancyCount = encounters.count { it.isPregnancy || it.isPregnancyMarked } + partner.myKids
        
        // Vaginal: thresholds [1,2,3,8,20]
        if (vaginalCount >= 1) {
            val lvl = when {
                vaginalCount >= 20 -> "Musa da Cama"
                vaginalCount >= 8 -> "Parceira Assídua"
                vaginalCount >= 3 -> "Novata"
                else -> "Estreante"
            }
            val textValue = if (vaginalCount == 1) lvl else "$lvl ($vaginalCount×)"
            addUniqueBadge("vaginal", "🔥", textValue)
        }

        // Anal: thresholds [1,5,15,30]
        if (analCount >= 1) {
            val lvl = when {
                analCount >= 30 -> "Deusa do Anal"
                analCount >= 15 -> "Rainha do Anal"
                analCount >= 5 -> "Musa do Anal"
                else -> "Exploradora"
            }
            addUniqueBadge("anal", "🍑", "$lvl ($analCount×)")
        }

        // Vaginal Creampie: thresholds [1,2,5,15,30]
        if (creampieCount >= 1) {
            val lvl = when {
                creampieCount >= 30 -> "Usina de Creme"
                creampieCount >= 15 -> "Recheio Quente"
                creampieCount >= 5 -> "Creme Caseiro"
                creampieCount >= 2 -> "Duplo Creme"
                else -> "Primeiro Recheio"
            }
            val textValue = if (creampieCount == 1) lvl else "$lvl ($creampieCount×)"
            addUniqueBadge("creampie", "💦 🍕", textValue)
        }

        // Anal Creampie: thresholds [1,5,15,30]
        if (analCreampieCount >= 1) {
            val lvl = when {
                analCreampieCount >= 30 -> "Apocalipse Anal"
                analCreampieCount >= 15 -> "Tsunami Anal"
                analCreampieCount >= 5 -> "Onda Anal"
                else -> "Toque Anal"
            }
            addUniqueBadge("anal_creampie", "🍑💦", "$lvl ($analCreampieCount×)")
        }

        // Oral: thresholds [1,5,15,30]
        if (oralCount >= 1) {
            val lvl = when {
                oralCount >= 30 -> "Boca Imortal"
                oralCount >= 15 -> "Mestre da Boca"
                oralCount >= 5 -> "Artista Oral"
                else -> "Boquinha Atrevida"
            }
            addUniqueBadge("oral", "😛", "$lvl ($oralCount×)")
        }

        // Facial: thresholds [1,5,10,20]
        if (facialCount >= 1) {
            val lvl = when {
                facialCount >= 20 -> "Lenda do Jato"
                facialCount >= 10 -> "Erupção Final"
                facialCount >= 5 -> "Tempestade Branca"
                else -> "Splash Suave"
            }
            addUniqueBadge("facial", "💦", "$lvl ($facialCount×)")
        }

        // Squirt: thresholds [1,3,8,15]
        if (squirtCount >= 1) {
            val lvl = when {
                squirtCount >= 15 -> "Deusa da Cachoeira"
                squirtCount >= 8 -> "Tsunami do Prazer"
                squirtCount >= 3 -> "Rainha do Jato"
                else -> "Fonte Oculta"
            }
            addUniqueBadge("squirt", "🌊", "$lvl ($squirtCount×)")
        }

        // Deepthroat: thresholds [1,5,10,20]
        if (deepthroatCount >= 1) {
            val lvl = when {
                deepthroatCount >= 20 -> "Lenda"
                deepthroatCount >= 10 -> "Expert"
                deepthroatCount >= 5 -> "Aventureira"
                else -> "Curiosa"
            }
            addUniqueBadge("deepthroat", "🕳️", "$lvl ($deepthroatCount×)")
        }

        // RELATIONSHIPS (Problema 2 - Critério híbrido)
        val totalSex = sexEncs.size
        val lastSex = sexEncs.maxByOrNull { it.date }
        val lastSexDays = if (lastSex != null) calculateDaysBetween(lastSex.date, getTodayDateString()) else 1000
        
        val relationshipDays = run {
            var maxDays = 0
            if (partner.firstDate.isNotEmpty()) {
                maxDays = maxOf(maxDays, calculateDaysBetween(partner.firstDate, getTodayDateString()))
            }
            encounters.forEach { enc ->
                if (enc.date.isNotEmpty()) {
                    maxDays = maxOf(maxDays, calculateDaysBetween(enc.date, getTodayDateString()))
                }
            }
            val createdAtStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(partner.createdAt))
            maxDays = maxOf(maxDays, calculateDaysBetween(createdAtStr, getTodayDateString()))
            maxDays
        }
        
        val isSingle = partner.availability.equals("Solteira", ignoreCase = true) || 
                partner.availability.isEmpty() || 
                partner.availability.equals("single", ignoreCase = true)
        
        if (isSingle && !partner.isGP) {
            if (totalSex >= 20 && relationshipDays > 730 && lastSexDays <= 45) {
                addUniqueBadge("wife", "💍", "Futura Esposa", isRelationship = true)
            } else if (totalSex >= 15 && relationshipDays >= 180 && relationshipDays <= 730 && lastSexDays <= 45) {
                addUniqueBadge("gf", "💕", "Namorada em Potencial", isRelationship = true)
            } else if (totalSex >= 3 && lastSexDays <= 60) {
                addUniqueBadge("ficante", "💙", "Ficante Assídua", isRelationship = true)
            }
        }

        // VOLATILE sex badge (Problema 3)
        if (isMostRecentSexGlobal) {
            addUniqueBadge("lastSex", "🔥", "Último Sexo", isVolatile = true, color = "#ef4444")
        }

        // PATERNITY / PREGNANCY
        val notesLowerPreg = partner.notes?.lowercase() ?: ""
        val isPregnant = notesLowerPreg.contains("grávida") || notesLowerPreg.contains("gravidez") || encounters.any { it.isPregnancy || it.isPregnancyMarked }
        if (isPregnant) {
            addUniqueBadge("mother_pregnant", "🤰", "Mulher Grávida (Nossos Filhos)")
        }

        if (pregnancyCount >= 3) {
            addUniqueBadge("pregnancy", "👨‍👧‍👦", "Patriarca (${pregnancyCount}f)")
        } else if (pregnancyCount == 2) {
            addUniqueBadge("pregnancy", "👨‍👧", "Pai Experiente (${pregnancyCount}f)")
        } else if (pregnancyCount == 1) {
            addUniqueBadge("pregnancy", "👶", "Pai Iniciante (${pregnancyCount}f)")
        }

        // NEGATIVES Checked Traços (Problema 6 - Sem duplicar emoji no texto)
        val negIds = (partner.negatives ?: "").split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        for (nid in negIds) {
            val trait = negativeTraitsList.find { it.id == nid }
            if (trait != null) {
                addUniqueBadge("neg_${trait.id}", trait.icon, trait.label, isNegative = true)
            }
        }

        return badges.filter { it.text.isNotEmpty() && it.icon.isNotEmpty() }
    }

    fun parseDateToMillis(dateStr: String): Long {
        val trimmed = dateStr.trim()
        if (trimmed.isEmpty()) return 0L
        return try {
            if (trimmed.contains("-")) {
                val parts = trimmed.split("-")
                if (parts.size >= 3 && parts[0].length == 4) {
                    val y = parts[0].toIntOrNull() ?: 1970
                    val m = (parts[1].toIntOrNull() ?: 1) - 1
                    val d = parts[2].substringBefore("T").substringBefore(" ").toIntOrNull() ?: 1
                    val cal = Calendar.getInstance()
                    cal.set(y, m, d, 12, 0, 0)
                    cal.timeInMillis
                } else {
                    0L
                }
            } else if (trimmed.contains("/")) {
                val parts = trimmed.split("/")
                if (parts.size >= 3 && parts[2].length >= 4) {
                    val d = parts[0].toIntOrNull() ?: 1
                    val m = (parts[1].toIntOrNull() ?: 1) - 1
                    val y = parts[2].substringBefore(" ").toIntOrNull() ?: 1970
                    val cal = Calendar.getInstance()
                    cal.set(y, m, d, 12, 0, 0)
                    cal.timeInMillis
                } else {
                    0L
                }
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    fun formatToBrazilianDate(dateStr: String): String {
        val trimmed = dateStr.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.contains("/")) return trimmed
        val parts = trimmed.split("-")
        if (parts.size == 3) {
            val y = parts[0]
            val m = parts[1]
            val d = parts[2]
            return "$d/$m/$y"
        }
        return trimmed
    }

    fun formatToStandardDate(dateStr: String): String {
        val trimmed = dateStr.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.contains("-")) return trimmed
        val parts = trimmed.split("/")
        if (parts.size == 3) {
            val d = parts[0]
            val m = parts[1]
            val y = parts[2]
            return "$y-$m-$d"
        }
        return trimmed
    }

    enum class WomanCategory(val displayName: String, val icon: String) {
        FUTURA_ESPOSA("Futura Esposa", "💍"),
        NAMORADA_POTENCIAL("Namorada em Potencial", "💕"),
        FICANTE_ASSIDUA("Ficante Assídua", "💙"),
        CASUAL("Casual / Em Aberto", "🌸"),
        GP("Profissional / GP", "👠")
    }

    fun calculateCategory(partner: Partner, encounters: List<Encontro>): WomanCategory {
        if (partner.isGP) return WomanCategory.GP
        
        val sexEncs = encounters.filter { it.hadSex }
        val totalSex = sexEncs.size
        val lastSex = sexEncs.maxByOrNull { parseDateToMillis(it.date) }
        val lastSexDays = if (lastSex != null) calculateDaysBetween(lastSex.date, getTodayDateString()) else 1000
        
        val relationshipDays = run {
            var maxDays = 0
            if (partner.firstDate.isNotEmpty()) {
                maxDays = maxOf(maxDays, calculateDaysBetween(partner.firstDate, getTodayDateString()))
            }
            encounters.forEach { enc ->
                if (enc.date.isNotEmpty()) {
                    maxDays = maxOf(maxDays, calculateDaysBetween(enc.date, getTodayDateString()))
                }
            }
            val createdAtStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(partner.createdAt))
            maxDays = maxOf(maxDays, calculateDaysBetween(createdAtStr, getTodayDateString()))
            maxDays
        }
        
        return when {
            totalSex >= 20 && relationshipDays > 730 && lastSexDays <= 45 -> WomanCategory.FUTURA_ESPOSA
            totalSex >= 15 && relationshipDays >= 180 && relationshipDays <= 730 && lastSexDays <= 45 -> WomanCategory.NAMORADA_POTENCIAL
            totalSex >= 3 && lastSexDays <= 60 -> WomanCategory.FICANTE_ASSIDUA
            else -> WomanCategory.CASUAL
        }
    }
}
