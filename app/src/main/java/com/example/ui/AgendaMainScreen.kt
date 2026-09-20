package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.provider.Settings
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch

@Composable
fun AgendaMainScreen(viewModel: AgendaViewModel) {
    val context = LocalContext.current
    val activeTab = viewModel.currentTab

    val bgApp = if (viewModel.isDarkTheme) DeepDarkBackground else Color(0xFFFAF6F0)
    val surfaceApp = if (viewModel.isDarkTheme) Level1DarkSurface else Color(0xFFFFFDF9)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = bgApp,
            topBar = {
                AgendaTopAppBar(
                    viewModel = viewModel,
                    currentTab = activeTab
                )
            },
            bottomBar = {
                AgendaBottomNavBar(
                    viewModel = viewModel,
                    currentTab = activeTab,
                    onTabSelected = { viewModel.currentTab = it }
                )
            },
            floatingActionButton = {
                if (activeTab == AppTab.MULHERES || activeTab == AppTab.AGENDA || activeTab == AppTab.GPS || activeTab == AppTab.FICHAS) {
                    FloatingActionButton(
                        onClick = {
                            if (activeTab == AppTab.MULHERES || activeTab == AppTab.FICHAS || activeTab == AppTab.GPS) {
                                if (activeTab == AppTab.GPS || (activeTab == AppTab.FICHAS && viewModel.selectedSubTab == 1)) {
                                    viewModel.gpToEdit = null
                                    viewModel.showAddGPDialog = true
                                } else {
                                    viewModel.partnerToEdit = null
                                    viewModel.showAddPartnerDialog = true
                                }
                            } else {
                                viewModel.showAddEncontroDialog = true
                            }
                        },
                        containerColor = PrimaryGold,
                        contentColor = OnPrimaryGold,
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 76.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar Novo"
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 0.dp,
                        start = innerPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        end = innerPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                    )
            ) {
                Crossfade(
                    targetState = activeTab,
                    animationSpec = spring(),
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        AppTab.INICIO -> DashboardScreen(viewModel = viewModel)
                        AppTab.MULHERES -> PartnersScreen(viewModel = viewModel)
                        AppTab.FICHAS -> PartnersScreen(viewModel = viewModel)
                        AppTab.COACH -> SeductionCoachScreen(viewModel = viewModel)
                        AppTab.AGENDA -> AgendaTimelineScreen(viewModel = viewModel)
                        AppTab.STATS -> StatsCardScreen(viewModel = viewModel)
                        AppTab.CONFIG -> SettingsScreen(viewModel = viewModel)
                        AppTab.GPS -> GPListScreen(viewModel = viewModel)
                        AppTab.DATE_SUGGESTIONS -> DateSuggestionsScreen(viewModel = viewModel)
                    }
                }

                if (viewModel.showAddPartnerDialog) {
                    AddOrEditWomanDialog(
                        partner = viewModel.partnerToEdit,
                        viewModel = viewModel,
                        onDismiss = {
                            viewModel.showAddPartnerDialog = false
                            viewModel.partnerToEdit = null
                        }
                    )
                }

                if (viewModel.showAddGPDialog) {
                    AddOrEditGPDialog(
                        partner = viewModel.gpToEdit,
                        viewModel = viewModel,
                        onDismiss = {
                            viewModel.showAddGPDialog = false
                            viewModel.gpToEdit = null
                        }
                    )
                }

                if (viewModel.showAddEncontroDialog) {
                    AddEncontroDialog(
                        viewModel = viewModel,
                        onDismiss = { 
                            viewModel.showAddEncontroDialog = false
                            viewModel.preselectedPartnerIdForEncontro = 0
                        }
                    )
                }
            }
        }

        if (viewModel.isAppLocked) {
            AgendaLockScreen(viewModel = viewModel)
        }
    }
}

@Composable
fun AgendaTopAppBar(
    viewModel: AgendaViewModel,
    currentTab: AppTab
) {
    val bgApp = if (viewModel.isDarkTheme) DeepDarkBackground else Color(0xFFFAF6F0)
    Surface(
        color = bgApp,
        modifier = Modifier.statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (viewModel.isDarkTheme) Level1DarkSurface else Color.White)
                        .border(1.dp, PrimaryGold.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Logo",
                        tint = PrimaryGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Agenda H",
                    color = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            IconButton(
                onClick = { viewModel.exportBackup() }
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Quick Backup",
                    tint = PrimaryGold
                )
            }
        }
    }
}

@Composable
fun AgendaBottomNavBar(
    viewModel: AgendaViewModel,
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = if (viewModel.isDarkTheme) Level1DarkSurface.copy(alpha = 0.92f) else Color(0xFFFBF8F3).copy(alpha = 0.92f),
            shape = RoundedCornerShape(100.dp),
            border = BorderStroke(1.dp, if (viewModel.isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = "Início",
                    isSelected = currentTab == AppTab.INICIO,
                    isSelectedTheme = viewModel.isDarkTheme,
                    onClick = { onTabSelected(AppTab.INICIO) }
                )
                
                BottomNavItem(
                    icon = Icons.Default.People,
                    label = "Mulheres",
                    isSelected = currentTab == AppTab.MULHERES || currentTab == AppTab.FICHAS,
                    isSelectedTheme = viewModel.isDarkTheme,
                    onClick = { onTabSelected(AppTab.MULHERES) }
                )
                
                if (viewModel.mostrarGP) {
                    BottomNavItem(
                        icon = Icons.Default.Star,
                        label = "GPs",
                        isSelected = currentTab == AppTab.GPS,
                        isSelectedTheme = viewModel.isDarkTheme,
                        onClick = { onTabSelected(AppTab.GPS) }
                    )
                } else {
                    BottomNavItem(
                        icon = Icons.Default.Forum,
                        label = "Coach IA",
                        isSelected = currentTab == AppTab.COACH,
                        isSelectedTheme = viewModel.isDarkTheme,
                        onClick = { onTabSelected(AppTab.COACH) }
                    )
                }

                BottomNavItem(
                    icon = Icons.Default.DateRange,
                    label = "Agenda",
                    isSelected = currentTab == AppTab.AGENDA,
                    isSelectedTheme = viewModel.isDarkTheme,
                    onClick = { onTabSelected(AppTab.AGENDA) }
                )
                BottomNavItem(
                    icon = Icons.Default.Settings,
                    label = "Config",
                    isSelected = currentTab == AppTab.CONFIG || currentTab == AppTab.STATS,
                    isSelectedTheme = viewModel.isDarkTheme,
                    onClick = { onTabSelected(AppTab.CONFIG) }
                )
            }
        }
    }
}

@Composable
fun RowScope.BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isSelectedTheme: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val activeColor = PrimaryGold
        val inactiveColor = if (isSelectedTheme) OnSurfaceGoldVariant.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.7f)
        val color = if (isSelected) activeColor else inactiveColor

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ------------------- SCREEN 1: DASHBOARD (HOME) -------------------

data class RankEntry(
    val keyName: String,
    val womenCount: Int,
    val sexCount: Int,
    val vaginalCount: Int,
    val analCount: Int
)

@Composable
fun DashboardScreen(viewModel: AgendaViewModel) {
    val partners by viewModel.allPartners.collectAsStateWithLifecycle()
    val encuentros by viewModel.allEncontros.collectAsStateWithLifecycle()
    val abstinence by viewModel.daysOfAbstinence.collectAsStateWithLifecycle()
    val lastPartner by viewModel.latestSexPartnerName.collectAsStateWithLifecycle("Sem registro")
    val lastEncounterCity by viewModel.latestEncounterCity.collectAsStateWithLifecycle("Sem registro")

    val normalSexCount by viewModel.normalSexCount.collectAsStateWithLifecycle()
    val normalAnalCount by viewModel.normalAnalCount.collectAsStateWithLifecycle()
    val normalCreampieCount by viewModel.normalCreampieCount.collectAsStateWithLifecycle()
    val uniqueWomenCount by viewModel.uniqueWomenWithSex.collectAsStateWithLifecycle()

    val normalSexThisYear by viewModel.normalSexThisYear.collectAsStateWithLifecycle()
    val normalAnalThisYear by viewModel.normalAnalThisYear.collectAsStateWithLifecycle()
    val partnersThisYear by viewModel.partnersThisYear.collectAsStateWithLifecycle()

    val conversionRate by viewModel.conversionRate.collectAsStateWithLifecycle()
    val efficiencyMeetings by viewModel.efficiencyMeetingsToSex.collectAsStateWithLifecycle()
    val avgMeetingsToSex by viewModel.averageMeetingsUntilSex.collectAsStateWithLifecycle()

    val gpCount by viewModel.gpEncountersCount.collectAsStateWithLifecycle()
    val gpSpent by viewModel.gpSpentTotal.collectAsStateWithLifecycle()
    val gpEstoque by viewModel.gpEstoquecount.collectAsStateWithLifecycle()
    val totalSpentMotel by viewModel.totalMotelCost.collectAsStateWithLifecycle()

    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
    val cardColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White
    val outlineBorderColor = if (viewModel.isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.12f)

    val currentAbsFilter = viewModel.abstinenceFilter
    val reactiveStats = remember(partners, encuentros, currentAbsFilter) {
        val targetPartners = partners.filter { p ->
            when (currentAbsFilter) {
                "Sem GP" -> !p.isGP
                "Só GP" -> p.isGP
                else -> true
            }
        }
        val targetIds = targetPartners.map { it.id }.toSet()
        val pEncs = encuentros.filter { targetIds.contains(it.partnerId) && it.hadSex }

        val totalSex = pEncs.size
        
        val vaginalSex = pEncs.sumOf { enc ->
            if (enc.typeVaginal || enc.gpSexoVaginal) {
                maxOf(1, enc.countVaginal)
            } else 0
        }
        
        val analSex = pEncs.sumOf { enc ->
            if (enc.typeAnal || enc.gpSexoAnal) {
                maxOf(1, enc.countAnal)
            } else 0
        }

        val partnersSexCount = targetPartners.count { p ->
            pEncs.any { it.partnerId == p.id }
        }

        val sexByYear = pEncs.groupBy { enc ->
            try {
                if (enc.date.contains("/")) {
                    enc.date.trim().split("/").last().toInt()
                } else if (enc.date.contains("-")) {
                    enc.date.trim().split("-").first().toInt()
                } else {
                    Calendar.getInstance().get(Calendar.YEAR)
                }
            } catch (e: Exception) {
                Calendar.getInstance().get(Calendar.YEAR)
            }
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentYearEncs = sexByYear[currentYear] ?: emptyList()
        val sexThisYear = currentYearEncs.size
        val vaginalThisYear = currentYearEncs.sumOf { enc ->
            if (enc.typeVaginal || enc.gpSexoVaginal) maxOf(1, enc.countVaginal) else 0
        }
        val analThisYear = currentYearEncs.sumOf { enc ->
            if (enc.typeAnal || enc.gpSexoAnal) maxOf(1, enc.countAnal) else 0
        }
        val currentYearPartnerIds = currentYearEncs.map { it.partnerId }.toSet()
        val partnersThisYear = targetPartners.count { currentYearPartnerIds.contains(it.id) }

        // Melhor Ano (ano recorde com maior quantidade de sexo, parceiras e com anal)
        val bestYearEntry = sexByYear.maxByOrNull { entry ->
            entry.value.size
        }
        val bestYear = bestYearEntry?.key ?: currentYear
        val bestYearEncs = bestYearEntry?.value ?: emptyList()
        val bestYearSex = bestYearEncs.size
        val bestYearPartnerIds = bestYearEncs.map { it.partnerId }.toSet()
        val bestYearPartners = targetPartners.count { bestYearPartnerIds.contains(it.id) }
        val bestYearAnal = bestYearEncs.sumOf { enc ->
            if (enc.typeAnal || enc.gpSexoAnal) maxOf(1, enc.countAnal) else 0
        }

        mapOf(
            "totalSex" to totalSex.toString(),
            "vaginalSex" to vaginalSex.toString(),
            "analSex" to analSex.toString(),
            "partnersCount" to partnersSexCount.toString(),
            "currentYear" to "$currentYear",
            "sexThisYear" to sexThisYear.toString(),
            "analThisYear" to analThisYear.toString(),
            "vaginalThisYear" to vaginalThisYear.toString(),
            "partnersThisYear" to partnersThisYear.toString(),
            "bestYear" to "$bestYear",
            "bestYearSex" to "$bestYearSex",
            "bestYearPartners" to "$bestYearPartners",
            "bestYearAnal" to "$bestYearAnal"
        )
    }

    // Pagers for sliding Stats and Rankings easily
    val statsPagerState = rememberPagerState(pageCount = { if (viewModel.mostrarGP) 4 else 3 })
    val rankingPagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel.mostrarGP) {
        val pageLimit = if (viewModel.mostrarGP) 4 else 3
        if (statsPagerState.currentPage >= pageLimit) {
            statsPagerState.scrollToPage(0)
        }
        if (!viewModel.mostrarGP && viewModel.abstinenceFilter == "Só GP") {
            viewModel.abstinenceFilter = "Total"
        }
    }

    LaunchedEffect(statsPagerState) {
        while (true) {
            kotlinx.coroutines.delay(8000)
            val pageCount = statsPagerState.pageCount
            if (pageCount > 0 && !statsPagerState.isScrollInProgress) {
                val currentSettled = statsPagerState.settledPage
                if (currentSettled < pageCount) {
                    val nextPage = (currentSettled + 1) % pageCount
                    try {
                        statsPagerState.animateScrollToPage(nextPage)
                    } catch (e: Exception) {
                        // Ignore scroll exceptions or cancellation
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val latestEncounterInfo = remember(partners, encuentros, viewModel.abstinenceFilter) {
            val sexEncontros = encuentros.filter { enc ->
                val p = partners.find { it.id == enc.partnerId }
                if (p == null) false else {
                    val matchesFilter = when (viewModel.abstinenceFilter) {
                        "Sem GP" -> !p.isGP
                        "Só GP" -> p.isGP
                        else -> true
                    }
                    matchesFilter && enc.hadSex && (enc.typeVaginal || enc.typeAnal || enc.typeOral)
                }
            }
            if (sexEncontros.isEmpty()) {
                null
            } else {
                sexEncontros.sortedByDescending { it.date }.firstOrNull()
            }
        }

        val lastSexDateStr = latestEncounterInfo?.date?.let { rawDate ->
            com.example.data.WomanCalculator.formatToBrazilianDate(rawDate)
        } ?: ""

        val lastSexPartnerName = latestEncounterInfo?.partnerName ?: ""

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PrimaryGold.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "DIAS EM ABSTINÊNCIA",
                    color = textVariantColor.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = if (abstinence == -1) "∞" else "$abstinence",
                    color = PrimaryGold,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                
                Text(
                    text = if (abstinence == 1) "DIA" else "DIAS",
                    color = textVariantColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (latestEncounterInfo != null) "Último: $lastSexPartnerName · $lastSexDateStr" else "Sem registro",
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                if (viewModel.mostrarGP) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Pills row
                    val currentAbsFilter = viewModel.abstinenceFilter
                    Row(
                        modifier = Modifier
                            .background(if (viewModel.isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .border(1.dp, PrimaryGold.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Total", "Sem GP", "Só GP").forEach { filterOpt ->
                            val isSelected = currentAbsFilter == filterOpt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSelected) PrimaryGold else Color.Transparent)
                                    .clickable { viewModel.abstinenceFilter = filterOpt }
                                    .padding(horizontal = 14.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filterOpt,
                                    color = if (isSelected) OnPrimaryGold else textVariantColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }



        // STATS ROTATOR - NOW WITH SWIPE GESTURES USING HORIZONTAL_PAGER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PrimaryGold.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header of panel indicating current card topic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when(statsPagerState.currentPage) {
                            0 -> "HISTÓRICO GERAL"
                            1 -> "TEMPORADA ATUAL (${reactiveStats["currentYear"] ?: ""})"
                            2 -> "MELHOR ANO"
                            else -> "FINANÇAS H"
                        },
                        color = PrimaryGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    // Visual slide indicators
                    val dotsCount = if (viewModel.mostrarGP) 4 else 3
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 0 until dotsCount) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (statsPagerState.currentPage == i) PrimaryGold else Color.Gray.copy(alpha = 0.4f))
                                    .clickable {
                                        coroutineScope.launch {
                                            if (i < statsPagerState.pageCount) {
                                                statsPagerState.animateScrollToPage(i)
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Render matching layout based on page with Swipable HorizontalPager
                 HorizontalPager(
                     state = statsPagerState,
                     modifier = Modifier.fillMaxWidth()
                 ) { page ->
                     when (page) {
                         0 -> {
                             Row(
                                 modifier = Modifier.fillMaxWidth().height(54.dp),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 MiniStatItem(label = "Sexo total", value = reactiveStats["totalSex"] ?: "0", modifier = Modifier.weight(1f))
                                 MiniStatItem(label = "Com anal", value = reactiveStats["analSex"] ?: "0", modifier = Modifier.weight(1f))
                                 MiniStatItem(label = "Vaginal", value = reactiveStats["vaginalSex"] ?: "0", modifier = Modifier.weight(1f))
                                 MiniStatItem(label = "Parceiras", value = reactiveStats["partnersCount"] ?: "0", modifier = Modifier.weight(1f))
                             }
                         }
                         1 -> {
                             Row(
                                 modifier = Modifier.fillMaxWidth().height(54.dp),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 MiniStatItem(label = "Sexo (${reactiveStats["currentYear"] ?: ""})", value = reactiveStats["sexThisYear"] ?: "0", modifier = Modifier.weight(1f))
                                 MiniStatItem(label = "Com anal", value = reactiveStats["analThisYear"] ?: "0", modifier = Modifier.weight(1f))
                                 MiniStatItem(label = "Vaginal", value = reactiveStats["vaginalThisYear"] ?: "0", modifier = Modifier.weight(1f))
                                 MiniStatItem(label = "Parceiras", value = reactiveStats["partnersThisYear"] ?: "0", modifier = Modifier.weight(1f))
                             }
                         }
                         2 -> {
                             Row(
                                 modifier = Modifier.fillMaxWidth().height(54.dp),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 MiniStatItem(label = "Ano Recorde", value = reactiveStats["bestYear"] ?: "-", modifier = Modifier.weight(1f))
                                 MiniStatItem(label = "Maior Sexo", value = reactiveStats["bestYearSex"] ?: "0", modifier = Modifier.weight(1f))
                                 MiniStatItem(label = "Parceiras", value = reactiveStats["bestYearPartners"] ?: "0", modifier = Modifier.weight(1f))
                                 MiniStatItem(label = "Com anal", value = reactiveStats["bestYearAnal"] ?: "0", modifier = Modifier.weight(1f))
                             }
                         }
                         else -> {
                             Row(
                                 modifier = Modifier.fillMaxWidth().height(54.dp),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 MiniStatItem(label = "Gasto Motéis", value = String.format("R$ %,.0f", totalSpentMotel), modifier = Modifier.weight(1f))
                                 MiniStatItem(label = "Gasto GP", value = String.format("R$ %,.0f", gpSpent), modifier = Modifier.weight(1f))
                                 MiniStatItem(label = "Encontros GP", value = "$gpCount", modifier = Modifier.weight(1f))
                                 MiniStatItem(label = "Estoque GP", value = "$gpEstoque", modifier = Modifier.weight(1f))
                             }
                         }
                     }
                 }
            }
        }

        // INVISIBLE COACH SYSTEM WARNINGS DECK (Sorted by importance)
        val alertsList = remember(partners, encuentros, abstinence, conversionRate, uniqueWomenCount, gpSpent) {
            val list = mutableListOf<CoachAlert>()
            
            // 🚨 Red / High Gravity alerts
            if (abstinence >= 15) {
                list.add(
                    CoachAlert(
                        title = "Inatividade Crítica Detectada!",
                        body = "Você está há $abstinence dias sem contato físico. O resfriamento de leads é severo. Retome papos no Tinder ou Instagram hoje mesmo!",
                        type = AlertType.CRITICAL
                    )
                )
            }
            encuentros.filter { it.isPregnancy }.forEach { enc ->
                val dateFr = com.example.data.WomanCalculator.formatToBrazilianDate(enc.date)
                list.add(
                    CoachAlert(
                        title = "🚨 Lembrete de Paternidade: ${enc.partnerName}",
                        body = "Atenção de Risco! Existe um registro de gravidez ou susto de paternidade em encontro com ${enc.partnerName} no dia $dateFr. Tome providências e controle o monitoramento!",
                        type = AlertType.CRITICAL
                    )
                )
            }

            // 💖 Dia dos Namorados (June 12th) and Relationship Anniversary alerts
            val todayCal = Calendar.getInstance()
            val todayMonth = todayCal.get(Calendar.MONTH) + 1
            val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

            // Dia dos Namorados: June 1st to June 12th (somente para ativas)
            if (todayMonth == 6 && todayDay <= 12) {
                partners.filter { !it.isGP && it.status == "Ativa" }.forEach { p ->
                    list.add(
                        CoachAlert(
                            title = "💖 Dia dos Namorados Chegando: ${p.name}",
                            body = "O Dia dos Namorados (12 de Junho) está se aproximando! Lembre-se de reservar motel/restaurante com antecedência para ${p.name} e preparar um presente especial para evitar conflitos!",
                            type = AlertType.WARNING
                        )
                    )
                }
            }

            // Dia das Mulheres (March 8th): March 1st to March 8th (somente para ativas)
            if (todayMonth == 3 && todayDay <= 8) {
                partners.filter { !it.isGP && it.status == "Ativa" }.forEach { p ->
                    list.add(
                        CoachAlert(
                            title = "🌹 Dia das Mulheres: ${p.name}",
                            body = "O Dia das Mulheres (8 de Março) está bem próximo! Demonstre carinho enviando uma mensagem de admiração e flores para sua parceira ativa ${p.name}!",
                            type = AlertType.INFO
                        )
                    )
                }
            }

            // Dia das Mães (May 2nd Sunday, estimate May 1st to May 14th): only for active mothers
            if (todayMonth == 5 && todayDay <= 14) {
                partners.filter { !it.isGP && it.status == "Ativa" && (it.children > 0 || it.myKids > 0) }.forEach { p ->
                    list.add(
                        CoachAlert(
                            title = "👩‍👧‍👦 Dia das Mães: ${p.name}",
                            body = "A ${p.name} é mãe e possui card ativo! No Dia das Mães, envie uma lembrança ou mensagem especial de carinho!",
                            type = AlertType.INFO
                        )
                    )
                }
            }

            // Anniversary check: first meet anniversary month and within 5 days of day (somente para ativas)
            partners.filter { !it.isGP && it.status == "Ativa" && it.firstDate.isNotEmpty() }.forEach { p ->
                val dateParts = if (p.firstDate.contains("-")) p.firstDate.split("-") else p.firstDate.split("/")
                if (dateParts.size == 3) {
                    val meetMonth = if (p.firstDate.contains("-")) (dateParts[1].toIntOrNull() ?: 0) else (dateParts[1].toIntOrNull() ?: 0)
                    val meetDay = if (p.firstDate.contains("-")) (dateParts[2].toIntOrNull() ?: 0) else (dateParts[0].toIntOrNull() ?: 0)
                    if (meetMonth == todayMonth && Math.abs(meetDay - todayDay) <= 5) {
                        list.add(
                            CoachAlert(
                                title = "🎉 Aniversário de Encontro: ${p.name}",
                                body = "No dia ${com.example.data.WomanCalculator.formatToBrazilianDate(p.firstDate)} ocorreu o seu primeiro encontro com ${p.name}. O aniversário de namoro está bem próximo! Garanta surpresas!",
                                type = AlertType.WARNING
                            )
                        )
                    }
                }
            }

            // ⚠️ Orange / Medium Alert
            // "contato esfriando" ou muitos dias de abstinência sexual (somente para ativas)
            partners.filter { !it.isGP && it.status == "Ativa" }.forEach { p ->
                val pEncs = encuentros.filter { it.partnerId == p.id }
                if (pEncs.isNotEmpty()) {
                    val latestEncDate = pEncs.map { it.date }.maxOrNull() ?: ""
                    if (latestEncDate.isNotEmpty()) {
                        val today = com.example.data.WomanCalculator.getTodayDateString()
                        val diffDays = com.example.data.WomanCalculator.calculateDaysBetween(latestEncDate, today)
                        if (diffDays >= 15) {
                            val latestSexDate = pEncs.filter { it.hadSex }.map { it.date }.maxOrNull() ?: ""
                            val pAbstinence = if (latestSexDate.isNotEmpty()) {
                                com.example.data.WomanCalculator.calculateDaysBetween(latestSexDate, today)
                            } else {
                                -1
                            }

                            val latestEncDateFr = com.example.data.WomanCalculator.formatToBrazilianDate(latestEncDate)
                            val alertBody = if (pAbstinence >= 15) {
                                "Já se passaram $diffDays dias desde o último encontro com ${p.name} ($latestEncDateFr) e $pAbstinence dias de abstinência sexual com ela. Contato ativo esfriando! Envie uma provocação no WhatsApp para reatar!"
                            } else {
                                "Já se passaram $diffDays dias desde o último encontro com ${p.name} ($latestEncDateFr). Não a deixe esfriar! Agende a próxima saída."
                            }

                            list.add(
                                CoachAlert(
                                    title = "🔥 Contato Esfriando: ${p.name}",
                                    body = alertBody,
                                    type = AlertType.WARNING
                                )
                            )
                        }
                    }
                }
            }

            // Dicas personalizadas com base no card da mulher (somente com contato ativo/ativa)
            partners.filter { !it.isGP && it.status == "Ativa" }.forEach { p ->
                // Tip on High Rating
                if (p.rating >= 9.0) {
                    list.add(
                        CoachAlert(
                            title = "💎 Alerta de Elite: ${p.name}",
                            body = "A ${p.name} possui rating excepcional (${p.rating}). Mantenha postura impecável, de alto valor H, e planeje dates requintados e privativos!",
                            type = AlertType.INFO
                        )
                    )
                }

                // Tip on Relationship Objective
                if (p.objective.contains("sério", ignoreCase = true)) {
                    list.add(
                        CoachAlert(
                            title = "💍 Postura de Alinhamento: ${p.name}",
                            body = "O foco de ${p.name} é 'Relacionamento sério'. Demonstre solidez, ambição profissional e estabilidade social!",
                            type = AlertType.INFO
                        )
                    )
                } else if (p.objective.contains("casual", ignoreCase = true)) {
                    list.add(
                        CoachAlert(
                            title = "🔥 Condução Casual: ${p.name}",
                            body = "Como ${p.name} busca algo 'Casual', suavize a formalidade. Mantenha as brincadeiras lúdicas e foco na atração física refinada!",
                            type = AlertType.INFO
                        )
                    )
                }

                // Tip on Negatives
                if (p.negatives.contains("fumante", ignoreCase = true)) {
                    list.add(
                        CoachAlert(
                            title = "🚬 Detalhe de Hábito: ${p.name}",
                            body = "${p.name} tem registro de fumante. Respeite as preferências dela, mas tenha sempre chicletes refrescantes para garantir o beijo H!",
                            type = AlertType.INFO
                        )
                    )
                }
                if (p.negatives.contains("ciumenta", ignoreCase = true) || p.negatives.contains("instavel", ignoreCase = true)) {
                    list.add(
                        CoachAlert(
                            title = "🧠 Controle Emocional: ${p.name}",
                            body = "Possíveis traços de instabilidade ou ciúmes. Evite discussões improdutivas por texto e lidere com paciência no presencial!",
                            type = AlertType.INFO
                        )
                    )
                }

                // Tip on Wine
                if (p.notes.contains("vinho", ignoreCase = true) || p.notes.contains("vinhos", ignoreCase = true)) {
                    list.add(
                        CoachAlert(
                            title = "🍷 Date sob Medida: ${p.name}",
                            body = "Ela gosta de vinhos. Proponha um encontro privativo com queijos e vinhos no seu espaço ou em um bistrô acolhedor!",
                            type = AlertType.INFO
                        )
                    )
                }
            }

            val goldOrSilverDulls = partners.filter { !it.isGP && (it.rating >= 8.5) && it.status == "Ativa" }.any { p ->
                val encs = encuentros.filter { it.partnerId == p.id }
                encs.isEmpty()
            }
            if (goldOrSilverDulls) {
                list.add(
                    CoachAlert(
                        title = "Oportunidade Pendente de Conclusão",
                        body = "Mulheres Ouro classificadas e ativas estão sem nenhum encontro registrado. Faça a abordagem prática.",
                        type = AlertType.WARNING
                    )
                )
            }

            // 💡 Blue / Informative Advice
            // "agendar encontro" (has whatsapp phone but 0 encounters)
            partners.filter { !it.isGP && it.phone.isNotEmpty() && it.status == "Ativa" }.forEach { p ->
                val hasNoEncs = encuentros.none { it.partnerId == p.id }
                if (hasNoEncs) {
                    list.add(
                        CoachAlert(
                            title = "Agendar Encontro: ${p.name}",
                            body = "Você possui o WhatsApp de ${p.name} (${p.phone}) mas nenhum encontro registrado com ela até hoje. Mova a interação adiante e marque uma saída!",
                            type = AlertType.INFO
                        )
                    )
                }
            }

            // Toxicity Check (Negative traits warning)
            partners.filter { !it.isGP && it.negatives.isNotEmpty() && it.status == "Ativa" }.forEach { p ->
                list.add(
                    CoachAlert(
                        title = "Análise de Risco: ${p.name}",
                        body = "${p.name} possui traços negativos cadastrados (${p.negatives}). Leve isso em consideração em sua próxima abordagem!",
                        type = AlertType.INFO
                    )
                )
            }

            if (conversionRate < 45.0 && partners.isNotEmpty()) {
                list.add(
                    CoachAlert(
                        title = "Dica de Filtragem Tinder",
                        body = "Sua taxa de conversão para sexo é de ${String.format("%.1f", conversionRate)}%. Melhore os critérios iniciais.",
                        type = AlertType.INFO
                    )
                )
            }
            if (avgMeetingsToSex > 3.0) {
                list.add(
                    CoachAlert(
                        title = "Otimizar tempo e reuniões",
                        body = "Sua média de encontros para sexo está em ${String.format("%.1f", avgMeetingsToSex)}. tente acelerar as conversas casuais.",
                        type = AlertType.INFO
                    )
                )
            }

            if (list.isEmpty()) {
                list.add(
                    CoachAlert(
                        title = "Sucesso Operacional Ativo",
                        body = "Operação H correndo de maneira exemplar, com agendas sob controle e finanças balanceadas. Parabéns!",
                        type = AlertType.INFO
                    )
                )
            }

            // Sort so critical (Red) comes first, warnings (Orange) second, info (Blue) third
            list.sortedBy {
                when(it.type) {
                    AlertType.CRITICAL -> 0
                    AlertType.WARNING -> 1
                    AlertType.INFO -> 2
                }
            }
        }

        // ROTATING RANKINGS CARD - SYSTEM SPEC 3
        val topWomen = remember(partners, encuentros) {
            partners.filter { !it.isGP }
                .map { partner ->
                    val pEncs = encuentros.filter { it.partnerId == partner.id }
                    partner to pEncs.size
                }
                .sortedByDescending { it.second }
                .take(3)
        }

        val topApps = remember(partners, encuentros) {
            partners.filter { !it.isGP && it.origin.isNotBlank() }
                .groupBy { it.origin.trim() }
                .map { (origin, list) ->
                    val womenCount = list.size
                    val pEncs = encuentros.filter { enc ->
                        val p = list.find { it.id == enc.partnerId }
                        p != null && enc.hadSex
                    }
                    val sexCount = pEncs.size
                    val vaginalCount = pEncs.sumOf { if (it.typeVaginal) maxOf(1, it.countVaginal) else 0 }
                    val analCount = pEncs.sumOf { if (it.typeAnal) maxOf(1, it.countAnal) else 0 }
                    RankEntry(origin, womenCount, sexCount, vaginalCount, analCount)
                }
                .sortedByDescending { it.sexCount }
                .take(3)
        }

         val topCities = remember(partners, encuentros) {
             partners.filter { !it.isGP }
                 .map { it to it.getCity() }
                 .filter { it.second.isNotBlank() && it.second != "Sem cidade" }
                 .groupBy { it.second.trim() }
                 .map { (location, listPair) ->
                     val list = listPair.map { it.first }
                     val womenCount = list.size
                     val pEncs = encuentros.filter { enc ->
                         val p = list.find { it.id == enc.partnerId }
                         p != null && enc.hadSex
                     }
                     val sexCount = pEncs.size
                     val vaginalCount = pEncs.sumOf { if (it.typeVaginal) maxOf(1, it.countVaginal) else 0 }
                     val analCount = pEncs.sumOf { if (it.typeAnal) maxOf(1, it.countAnal) else 0 }
                     RankEntry(location, womenCount, sexCount, vaginalCount, analCount)
                 }
                 .sortedByDescending { it.sexCount }
                 .take(3)
         }

        LaunchedEffect(rankingPagerState) {
            while (true) {
                kotlinx.coroutines.delay(8000L) // 8 seconds per slide
                val pageCount = rankingPagerState.pageCount
                if (pageCount > 0 && !rankingPagerState.isScrollInProgress) {
                    val currentSettled = rankingPagerState.settledPage
                    if (currentSettled < pageCount) {
                        val nextPage = (currentSettled + 1) % pageCount
                        try {
                            rankingPagerState.animateScrollToPage(nextPage)
                        } catch (e: Exception) {
                            // Ignore scroll exceptions or cancellation
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Melhores do H (Rankings)",
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PrimaryGold.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when(rankingPagerState.currentPage) {
                                0 -> "👑 RANKING: TOP 3 MULHERES (ENCONTROS)"
                                1 -> "📱 RANKING: TOP 3 APPS (CONVERSÕES)"
                                else -> "🌆 RANKING: TOP 3 CIDADES (CONVERSÕES)"
                            },
                            color = PrimaryGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0, 1, 2).forEach { page ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (rankingPagerState.currentPage == page) PrimaryGold else Color.Gray.copy(alpha = 0.4f))
                                        .clickable {
                                            coroutineScope.launch {
                                                rankingPagerState.animateScrollToPage(page)
                                            }
                                        }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalPager(
                        state = rankingPagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (page) {
                                0 -> {
                                    if (topWomen.isEmpty()) {
                                        Text("Dados insuficientes para gerar o ranking.", color = textVariantColor, fontSize = 12.sp)
                                    } else {
                                        topWomen.forEachIndexed { rank, (woman, score) ->
                                            val medal = when(rank) {
                                                0 -> "🥇"
                                                1 -> "🥈"
                                                else -> "🥉"
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = medal, fontSize = 20.sp, modifier = Modifier.width(32.dp))
                                                    Column {
                                                        Text(text = woman.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                        Text(text = "${woman.origin} • ${woman.getCity()}", color = textVariantColor, fontSize = 10.sp)
                                                    }
                                                }
                                                Text(text = "$score ${if (score == 1) "encontro" else "encontros"}", color = PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                            }
                                            if (rank < topWomen.size - 1) {
                                                HorizontalDivider(color = outlineBorderColor, thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    if (topApps.isEmpty()) {
                                        Text("Dados insuficientes para gerar o ranking.", color = textVariantColor, fontSize = 12.sp)
                                    } else {
                                        topApps.forEachIndexed { rank, entry ->
                                            val (origin, womenVal, sexVal, vagVal, analVal) = entry
                                            val medal = when(rank) {
                                                0 -> "🥇"
                                                1 -> "🥈"
                                                else -> "🥉"
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = medal, fontSize = 20.sp, modifier = Modifier.width(32.dp))
                                                    Column {
                                                        Text(text = origin, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                        Text(text = "$womenVal contatos", color = textVariantColor, fontSize = 11.sp)
                                                    }
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(text = "$vagVal Vaginal", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text(text = "$analVal Anal", color = textVariantColor, fontSize = 11.sp)
                                                }
                                            }
                                            if (rank < topApps.size - 1) {
                                                HorizontalDivider(color = outlineBorderColor, thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    if (topCities.isEmpty()) {
                                        Text("Dados insuficientes para gerar o ranking.", color = textVariantColor, fontSize = 12.sp)
                                    } else {
                                        topCities.forEachIndexed { rank, entry ->
                                            val (loc, womenVal, sexVal, vagVal, analVal) = entry
                                            val medal = when(rank) {
                                                0 -> "🥇"
                                                1 -> "🥈"
                                                else -> "🥉"
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = medal, fontSize = 20.sp, modifier = Modifier.width(32.dp))
                                                    Column {
                                                        Text(text = loc, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                        Text(text = "$womenVal contatos", color = textVariantColor, fontSize = 11.sp)
                                                    }
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(text = "$vagVal Vaginal", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text(text = "$analVal Anal", color = textVariantColor, fontSize = 11.sp)
                                                }
                                            }
                                            if (rank < topCities.size - 1) {
                                                HorizontalDivider(color = outlineBorderColor, thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ROTATING SINGLE ALERT SYSTEM - SYSTEM SPEC 1
        var activeAlertIndex by remember(alertsList) { mutableStateOf(0) }
        LaunchedEffect(alertsList) {
            if (alertsList.size > 1) {
                while (true) {
                    kotlinx.coroutines.delay(12000L) // 12 seconds of reading time
                    activeAlertIndex = (activeAlertIndex + 1) % alertsList.size
                }
            } else {
                activeAlertIndex = 0
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conselho do Invisible Coach",
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (alertsList.isNotEmpty()) {
                    Text(
                        text = "${activeAlertIndex + 1} de ${alertsList.size}",
                        color = textVariantColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (alertsList.isNotEmpty()) {
                val alert = alertsList.getOrNull(activeAlertIndex) ?: alertsList[0]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            when (alert.type) {
                                AlertType.CRITICAL -> Color(0xFFE57373).copy(alpha = 0.4f)
                                AlertType.WARNING -> Color(0xFFFFB74D).copy(alpha = 0.4f)
                                AlertType.INFO -> PrimaryGold.copy(alpha = 0.4f)
                            },
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = when (alert.type) {
                            AlertType.CRITICAL -> Color(0xFF2C1616)
                            AlertType.WARNING -> Color(0xFF2E2214)
                            AlertType.INFO -> cardColor
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (alertsList.size > 1) {
                            IconButton(
                                onClick = {
                                    activeAlertIndex = if (activeAlertIndex > 0) activeAlertIndex - 1 else alertsList.size - 1
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Prev",
                                    tint = textVariantColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = when (alert.type) {
                                        AlertType.CRITICAL -> "🚨 CRÍTICO:"
                                        AlertType.WARNING -> "⚠️ ATENÇÃO:"
                                        AlertType.INFO -> "💡 CONSELHO:"
                                    },
                                    color = when (alert.type) {
                                        AlertType.CRITICAL -> Color(0xFFEF5350)
                                        AlertType.WARNING -> Color(0xFFFFA726)
                                        AlertType.INFO -> PrimaryGold
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = alert.title,
                                    color = textColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = alert.body,
                                color = textVariantColor.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        if (alertsList.size > 1) {
                            IconButton(
                                onClick = {
                                    activeAlertIndex = (activeAlertIndex + 1) % alertsList.size
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Next",
                                    tint = textVariantColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // BENTO STYLE QUICK ACTIONS
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Quartel General H",
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sugestão de Dates Button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.currentTab = AppTab.DATE_SUGGESTIONS }
                        .border(1.dp, outlineBorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = PrimaryGold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Sugestões de Encontros", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Cafés, bares, motéis em SP", color = textVariantColor, fontSize = 11.sp)
                    }
                }

                // Coach IA Button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.currentTab = AppTab.COACH }
                        .border(1.dp, outlineBorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(imageVector = Icons.Default.Forum, contentDescription = null, tint = PrimaryGold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Coach IA Invisível", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Análise de conversas e fotos", color = textVariantColor, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

enum class AlertType { CRITICAL, WARNING, INFO }
data class CoachAlert(val title: String, val body: String, val type: AlertType)

@Composable
fun MiniStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(text = value, color = OnSurfaceGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = OnSurfaceGoldVariant.copy(alpha = 0.6f), fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

// ------------------- SCREEN 2: PARTNERS LIST (MULHERES) -------------------

@Composable
fun PartnersScreen(viewModel: AgendaViewModel) {
    val search = viewModel.partnerSearchQuery
    val activeFilter = viewModel.activePartnerFilter
    val partners by viewModel.filteredPartners.collectAsStateWithLifecycle()
    val encuentros by viewModel.allEncontros.collectAsStateWithLifecycle()
    var activeDetailPartner by remember { mutableStateOf<Partner?>(null) }
    var encounterToEdit by remember { mutableStateOf<Encontro?>(null) }
    var partnerToDelete by remember { mutableStateOf<Partner?>(null) }

    val encountersByPartner = remember(encuentros) {
        encuentros.groupBy { it.partnerId }
    }

    // Precalculate ranking index for all partners to avoid performance bottleneck inside Column items mapping
    val rankMap = remember(partners, encountersByPartner) {
        partners.sortedByDescending { targetPartner ->
            val pEncs = encountersByPartner[targetPartner.id] ?: emptyList()
            WomanCalculator.calculateScoreW(targetPartner, pEncs)
        }.mapIndexed { index, partner ->
            partner.id to (index + 1)
        }.toMap()
    }
    val context = LocalContext.current

    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
    val cardColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = { viewModel.partnerSearchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            placeholder = {
                Text(
                    text = "Buscar contatos, cidades...",
                    color = textVariantColor.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Busca",
                    tint = PrimaryGold
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = PrimaryGold,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.15f)
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PartnerFilter.values().forEach { filter ->
                val isSelected = activeFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            if (isSelected) PrimaryGold.copy(alpha = 0.15f) else cardColor
                        )
                        .border(
                            1.dp,
                            if (isSelected) PrimaryGold else Color.LightGray.copy(alpha = 0.15f),
                            RoundedCornerShape(100.dp)
                        )
                        .clickable { viewModel.activePartnerFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) PrimaryGold else textVariantColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Divider and Label for Sorting
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            )

            Text(
                text = "Ordenar:",
                color = textVariantColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            PartnerSortOption.values().forEach { option ->
                val isSelected = viewModel.partnerSortOption == option
                val label = when (option) {
                    PartnerSortOption.ABSTINENCIA -> "Abstinência ⏳"
                    PartnerSortOption.RANKING -> "Ranking 👑"
                    PartnerSortOption.NOME -> "Nome 🔤"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (isSelected) PrimaryGold.copy(alpha = 0.15f) else cardColor)
                        .border(
                            1.dp,
                            if (isSelected) PrimaryGold else Color.LightGray.copy(alpha = 0.15f),
                            RoundedCornerShape(100.dp)
                        )
                        .clickable { viewModel.partnerSortOption = option }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) PrimaryGold else textVariantColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Lazy column cards list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (partners.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma mulher cadastrada com esses filtros.",
                            color = textVariantColor.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(partners, key = { it.id }) { partner ->
                    val partnerEncontros = remember(partner.id, encountersByPartner) {
                        encountersByPartner[partner.id] ?: emptyList()
                    }
                    
                    val bGroup = remember(partner, partnerEncontros) {
                        WomanCalculator.calculateBorderColorGroup(partner, partnerEncontros)
                    }
                    val borderColor = remember(bGroup) {
                        when (bGroup) {
                            "Ouro" -> Color(0xFFFFD700)
                            "Prata" -> Color(0xFFC0C0C0)
                            "Bronze" -> Color(0xFFCD7F32)
                            "Verde" -> Color(0xFF4CAF50)
                            "Amarelo" -> Color(0xFFFFEB3B)
                            "Vermelho" -> Color(0xFFF44336)
                            else -> Color.Gray.copy(alpha = 0.4f)
                        }
                    }

                        val sexEncs = remember(partnerEncontros) { partnerEncontros.filter { it.hadSex } }
                        val lastSexDate = remember(sexEncs) { sexEncs.maxByOrNull { it.date }?.date }
                        val daysNoSex = remember(lastSexDate) {
                            if (lastSexDate != null) {
                                com.example.data.WomanCalculator.calculateDaysBetween(lastSexDate, com.example.data.WomanCalculator.getTodayDateString())
                            } else {
                                -1
                            }
                        }

                        val relationshipDuration = remember(partner.firstDate) { calculateRelationshipDuration(partner.firstDate) }
                        val relationshipText = remember(relationshipDuration) { if (relationshipDuration.isNotEmpty()) " ($relationshipDuration)" else "" }

                        val finalAge = remember(partner) { WomanCalculator.calculateCurrentAge(partner) }
                        val scoreW = remember(partner, partnerEncontros) { WomanCalculator.calculateScoreW(partner, partnerEncontros) }
                        val rankIndex = remember(partner.id, rankMap) { rankMap[partner.id] ?: 99 }

                        val firstDateFormatted = remember(partner.firstDate) { com.example.data.WomanCalculator.formatToBrazilianDate(partner.firstDate) }

                        var isSuggestionsExpanded by remember { mutableStateOf(false) }
                        var isHistoryExpanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(if (bGroup == "Cinza") 1.dp else 2.dp, borderColor), RoundedCornerShape(16.dp))
                                .clickable { activeDetailPartner = partner },
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                            // FIFA Player Card Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // FIFA Left Portrait Frame
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .background(PrimaryGold.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                        .width(60.dp)
                                ) {
                                    // Overall Rating -> Swapped to current age
                                    Text(
                                        text = "$finalAge",
                                        color = PrimaryGold,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    // Class Name / Graduation -> Swapped label to ANOS
                                    Text(
                                        text = "ANOS",
                                        color = borderColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Avatar Frame
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, borderColor, CircleShape)
                                            .background(Level2DarkSurface)
                                    ) {
                                        if (partner.photoUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = partner.photoUrl,
                                                contentDescription = partner.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                val rankText = when (rankIndex) {
                                                    1 -> "🥇"
                                                    2 -> "🥈"
                                                    3 -> "🥉"
                                                    else -> "${rankIndex}º"
                                                }
                                                Text(rankText, color = PrimaryGold, fontSize = if (rankIndex <= 3) 22.sp else 14.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    // Small Favorite Star icon
                                    if (partner.isFavorite) {
                                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(12.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Right side details
                                Column(modifier = Modifier.weight(1f)) {
                                    // Name & Delete button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = partner.name,
                                            color = textColor,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val newStatus = if (partner.status == "Ativa") "Inativa" else "Ativa"
                                                    viewModel.updatePartner(partner.copy(status = newStatus))
                                                    Toast.makeText(context, "${partner.name} foi ${if (newStatus == "Inativa") "desativada" else "ativada"}!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (partner.status == "Ativa") Icons.Default.PowerSettingsNew else Icons.Default.CheckCircle,
                                                    contentDescription = if (partner.status == "Ativa") "Desativar" else "Ativar",
                                                     tint = if (partner.status == "Ativa") Color(0xFFEF5350).copy(alpha = 0.8f) else Color(0xFF4CAF50).copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { partnerToDelete = partner },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Excluir Cadastro",
                                                    tint = Color.Red.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Date of first encounter & age at first encounter
                                    Text(
                                        text = "📅 1º Enc: $firstDateFormatted • Tinha ${partner.age} anos",
                                        color = textVariantColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Children meus / outros info
                                    Text(
                                        text = "👶 Filhos: meus ${partner.myKids} ; outros ${partner.children}",
                                        color = textVariantColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Abstinence formatting in Year/Month/Day
                                    val formattedAbstinence = com.example.data.WomanCalculator.formatAbstinenceYMD(daysNoSex)
                                    Text(
                                        text = "⌛ Abstinência: $formattedAbstinence",
                                        color = PrimaryGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Details Row
                                    Text(
                                        text = "${partner.origin} • ${partner.getCity()} • ${partner.availability}",
                                        color = textVariantColor.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    

                                }
                            }

                            if (partner.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = partner.notes,
                                    color = textVariantColor.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Dynamic Badges List
                            val isMostRecentReg = remember(partner.id, encuentros) {
                                viewModel.checkIsMostRecentSexGlobal(partner.id, encuentros)
                            }
                            val badges = remember(partner, partnerEncontros, isMostRecentReg) {
                                WomanCalculator.calculateBadges(partner, partnerEncontros, isMostRecentReg)
                            }
                            if (badges.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    badges.forEach { b ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (b.isNegative) Color(0x22EF5350)
                                                    else if (b.isRelationship) Color(0x33BBDEFB)
                                                    else if (b.isVolatile) Color(0x33FFB74D)
                                                    else PrimaryGold.copy(alpha = 0.12f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (b.isNegative) Color(0xFFEF5350).copy(alpha = 0.3f)
                                                    else if (b.isRelationship) Color(0xFF1E88E5).copy(alpha = 0.3f)
                                                    else if (b.isVolatile) Color(0xFFFFA726).copy(alpha = 0.3f)
                                                    else PrimaryGold.copy(alpha = 0.3f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${b.icon} ${b.text}",
                                                color = if (b.isNegative) Color(0xFFE57373)
                                                else if (b.isRelationship) Color(0xFF64B5F6)
                                                else if (b.isVolatile) Color(0xFFFFB74D)
                                                else PrimaryGold,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // COLLAPSIBLE TRYS
                            // 1. Suggestions Collapse Tray
                            if (isSuggestionsExpanded) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("SUGESTÕES DE ENCONTROS (SP) 🗺️", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        val recommendations = mutableListOf<Triple<String, String, String>>()
                                        if (partner.objective == "Relacionamento sério" || partner.affinity >= 72) {
                                            recommendations.add(Triple("L'Entrecôte de Paris (Itaim)", "🍽️ Jantar Especial", "Ambiente decorado parisiense, intimista e clássico. Super recomendado para conexões profundas e impressionar."))
                                            recommendations.add(Triple("Santo Grão (Oscar Freire)", "☕ Café & Prosa", "Decoração sofisticada no Jardins, perfeito para conversas agradáveis no meio da tarde com conforto."))
                                        } else {
                                            recommendations.add(Triple("Bar do Cofre SubAstor", "🍸 Drinks Exclusivos", "Cocktails icônicos localizados embaixo do Centro. Cenógrafo perfeito para o clima de romance casual."))
                                            recommendations.add(Triple("Patisserie Douce France", "🍰 Primeiro Date Rápido", "Café parisiense com doces incríveis na Oscar Freire, ideal para encontros descontraídos e check-in rápido."))
                                        }
                                        val hasSexHistory = partnerEncontros.any { it.hadSex }
                                        if (hasSexHistory || badges.any { it.type == "creampie" || it.type == "anal" }) {
                                            recommendations.add(Triple("Motel Lush (Ipiranga)", "🔞 Suítes Premium", "Suítes tecnológicas, teto panorâmico e climatizadores especiais. Melhor experiência para comemorar em grande estilo."))
                                        } else {
                                            recommendations.add(Triple("Parque do Ibirapuera", "🌳 Encontro Descontraído", "Caminhar ao entardecer à beira do lago. Sem pressão fiscal, excelente para rir, conversar e aproximar."))
                                        }

                                        recommendations.forEach { spot ->
                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = spot.first, color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text(text = spot.second, color = textColor, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Text(text = spot.third, color = textVariantColor, fontSize = 10.sp, lineHeight = 14.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Encounter history Collapse Tray
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isHistoryExpanded = !isHistoryExpanded }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Histórico de Encontros (${partnerEncontros.size})",
                                        color = textColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (isHistoryExpanded) "Recolher ▲" else "Expandir ▼",
                                    color = PrimaryGold,
                                    fontSize = 10.sp
                                )
                            }

                            if (isHistoryExpanded) {
                                if (partnerEncontros.isEmpty()) {
                                    Text(
                                        text = "Nenhum encontro registrado ainda.",
                                        color = textVariantColor.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        partnerEncontros.forEach { encontro ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.5f)),
                                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Text(
                                                                text = "📅 ${com.example.data.WomanCalculator.formatToBrazilianDate(encontro.date)}",
                                                                color = textColor,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "★ ${encontro.rating}",
                                                                color = PrimaryGold,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Edit button with golden yellow border
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(width = 34.dp, height = 24.dp)
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .border(1.dp, PrimaryGold.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                                    .clickable { encounterToEdit = encontro },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(text = "✏️", fontSize = 10.sp)
                                                            }
                                                            
                                                            // Delete button with red border
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(width = 34.dp, height = 24.dp)
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                                    .clickable {
                                                                        viewModel.deleteEncontro(encontro)
                                                                        Toast.makeText(context, "Encontro removido!", Toast.LENGTH_SHORT).show()
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(text = "🗑️", fontSize = 10.sp)
                                                            }
                                                        }
                                                    }
                                                    if (encontro.notes.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = encontro.notes,
                                                            color = textColor.copy(alpha = 0.8f),
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    // Render activity summary indicators with wrapping and counts
                                                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    ) {
                                                        if (encontro.typeVaginal) {
                                                            val countLabel = if (encontro.countVaginal > 1) " (${encontro.countVaginal}x)" else ""
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(PrimaryGold.copy(alpha = 0.13f))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("🔥 vaginal$countLabel", color = textColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        if (encontro.typeAnal) {
                                                            val countLabel = if (encontro.countAnal > 1) " (${encontro.countAnal}x)" else ""
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(Color(0xFFFFB74D).copy(alpha = 0.13f))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("🍑 anal$countLabel", color = Color(0xFFFFB74D), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        if (encontro.typeCreampie) {
                                                            val countLabel = if (encontro.countCreampie > 1) " (${encontro.countCreampie}x)" else ""
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(Color(0xFFEF5350).copy(alpha = 0.13f))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(if (partner.isGP) "💋 beijos na boca$countLabel" else "💦 🍕 creampie$countLabel", color = Color(0xFFEF5350), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        if (encontro.typeAnalCreampie) {
                                                            val countLabel = if (encontro.countAnalCreampie > 1) " (${encontro.countAnalCreampie}x)" else ""
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(Color(0xFFBA68C8).copy(alpha = 0.13f))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("🍑💦 anal creampie$countLabel", color = Color(0xFFBA68C8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        if (encontro.typeOral) {
                                                            val countLabel = if (encontro.countOral > 1) " (${encontro.countOral}x)" else ""
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(PrimaryGold.copy(alpha = 0.13f))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(if (partner.isGP) "👄 oral s/camisinha$countLabel" else "😛 oral$countLabel", color = textColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        if (encontro.typeFacial) {
                                                            val countLabel = if (encontro.countFacial > 1) " (${encontro.countFacial}x)" else ""
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(Color(0xFFFFF176).copy(alpha = 0.13f))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(if (partner.isGP) "🔄 69 c/camisinha$countLabel" else "💦 facial$countLabel", color = Color(0xFFFFF176), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        if (encontro.typeSquirt) {
                                                            val countLabel = if (encontro.countSquirt > 1) " (${encontro.countSquirt}x)" else ""
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(Color(0xFF4FC3F7).copy(alpha = 0.13f))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(if (partner.isGP) "💆‍♀️ massagem erótica$countLabel" else "🌊 squirt$countLabel", color = Color(0xFF4FC3F7), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        if (encontro.typeDeepthroat) {
                                                            val countLabel = if (encontro.countDeepthroat > 1) " (${encontro.countDeepthroat}x)" else ""
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(Color(0xFFBA68C8).copy(alpha = 0.13f))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(if (partner.isGP) "⛓️ dominação / bdsm$countLabel" else "👅 profunda$countLabel", color = Color(0xFFBA68C8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action buttons grid (5 buttons)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 1. Sugestão de Date
                                Button(
                                    onClick = {
                                        viewModel.navigateToDateSuggestions(partner.id, "Casa dela")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.05f),
                                        contentColor = PrimaryGold
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1.1f).height(40.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(imageVector = Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text("Date 🍽️", fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }

                                // 2. Registrar Encontro
                                Button(
                                    onClick = {
                                        viewModel.preselectedPartnerIdForEncontro = partner.id
                                        viewModel.showAddEncontroDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1.2f).height(40.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text("Encontro 🔞", fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }

                                // 3. WhatsApp Button (Whats 💬)
                                Button(
                                    onClick = {
                                        try {
                                            val cleanPhone = partner.phone.replace("[^0-9]".toRegex(), "")
                                            val finalPhone = if (cleanPhone.length <= 11) "55$cleanPhone" else cleanPhone
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$finalPhone"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                             Toast.makeText(context, "WhatsApp indisponível.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = partner.phone.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (partner.phone.isNotEmpty()) Color(0xFF25D366) else Color.White.copy(alpha = 0.05f),
                                        contentColor = if (partner.phone.isNotEmpty()) Color.White else Color.Gray
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text("Whats 💬", fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }

                                // 4. Waze Button
                                Button(
                                    onClick = {
                                        if (partner.address.isNotEmpty()) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("waze://?q=${Uri.encode(partner.address)}&navigate=yes"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=${Uri.encode(partner.address)}&navigate=yes"))
                                                context.startActivity(fallbackIntent)
                                            }
                                        } else {
                                            Toast.makeText(context, "Cadastre o endereço dela para navegar!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00C1FF), 
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text("Waze 🗺️", fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }

                                // 5. Editar Button
                                Button(
                                    onClick = {
                                        viewModel.partnerToEdit = partner
                                        viewModel.showAddPartnerDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        contentColor = textColor
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text("Editar ✏️", fontSize = 8.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }

    if (activeDetailPartner != null) {
        WomanDetailDialog(
            partner = activeDetailPartner!!,
            onDismiss = { activeDetailPartner = null },
            viewModel = viewModel
        )
    }

    if (encounterToEdit != null) {
        EditEncontroDialog(
            encontro = encounterToEdit!!,
            onDismiss = { encounterToEdit = null },
            onConfirm = { updated ->
                viewModel.updateEncontro(updated)
                encounterToEdit = null
            },
            isGP = false,
            viewModel = viewModel
        )
    }

    if (partnerToDelete != null) {
        AlertDialog(
            onDismissRequest = { partnerToDelete = null },
            title = { Text(text = "Excluir Cadastro", fontWeight = FontWeight.Bold, color = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)) },
            text = { Text(text = "Tem certeza que deseja excluir o cadastro de ${partnerToDelete?.name}? Essa ação não poderá ser desfeita.", color = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)) },
            confirmButton = {
                Button(
                    onClick = {
                        partnerToDelete?.let {
                            viewModel.deletePartner(it)
                        }
                        partnerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { partnerToDelete = null }) {
                    Text("Cancelar", color = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B))
                }
            },
            containerColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White,
            tonalElevation = 6.dp
        )
    }
}

// ------------------- SCREEN 3: CALENDAR & TIMELINE (AGENDA) -------------------

@Composable
fun AgendaTimelineScreen(viewModel: AgendaViewModel) {
    val encuentros by viewModel.allEncontros.collectAsStateWithLifecycle()
    val partners by viewModel.allPartners.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current

    val encountersByDate = remember(encuentros) {
        encuentros.groupBy { it.date }
    }
    val partnersById = remember(partners) {
        partners.associateBy { it.id }
    }

    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
    val cardColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White

    // Calendar state tracking monthly view
    var calendarMonthOffset by remember { mutableStateOf(0) }
    val displayCalendar = remember(calendarMonthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, calendarMonthOffset)
        cal
    }
    val currentMonthYearString = remember(calendarMonthOffset) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
        sdf.format(displayCalendar.time).replaceFirstChar { it.uppercase() }
    }

    var selectedDayStr by remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        mutableStateOf(sdf.format(Date()))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // MONTHLY CALENDAR GRID
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Selector Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { calendarMonthOffset-- }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = PrimaryGold)
                    }
                    Text(
                        text = currentMonthYearString,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { calendarMonthOffset++ }) {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Days of week row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val daysOfWeekLabels = listOf("D", "S", "T", "Q", "Q", "S", "S")
                    daysOfWeekLabels.forEach { label ->
                        Text(
                            text = label,
                            color = textVariantColor.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Render Month Grid
                val gridCells = remember(calendarMonthOffset) {
                    val cells = mutableListOf<String>()
                    val tempCal = displayCalendar.clone() as Calendar
                    tempCal.set(Calendar.DAY_OF_MONTH, 1)
                    val firstDayDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) // 1-indexed (Sun=1)
                    val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                    // Prefix blanks
                    for (i in 1 until firstDayDayOfWeek) {
                        cells.add("")
                    }
                    // Day count strings (yyyy-MM-dd formatted)
                    val sdfFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    for (day in 1..daysInMonth) {
                        tempCal.set(Calendar.DAY_OF_MONTH, day)
                        cells.add(sdfFormat.format(tempCal.time))
                    }
                    cells
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val rowsCount = (gridCells.size + 6) / 7
                    for (r in 0 until rowsCount) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            for (c in 0 until 7) {
                                val idx = r * 7 + c
                                if (idx < gridCells.size) {
                                    val dateStr = gridCells[idx]
                                    if (dateStr.isEmpty()) {
                                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        val parseDay = dateStr.split("-").last().toInt()
                                        val isSelected = dateStr == selectedDayStr
                                        
                                        // Query standard encounters matching date
                                        val dayEncontros = encountersByDate[dateStr] ?: emptyList()
                                        val hasSex = dayEncontros.any { it.hadSex && (it.typeVaginal || it.typeAnal || it.typeOral) }
                                        val hasNormal = dayEncontros.isNotEmpty()

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) PrimaryGold.copy(alpha = 0.2f)
                                                    else Color.Transparent
                                                )
                                                .border(
                                                    if (isSelected) BorderStroke(1.dp, PrimaryGold) else BorderStroke(0.dp, Color.Transparent),
                                                    CircleShape
                                                )
                                                .clickable { selectedDayStr = dateStr }
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                Text(
                                                    text = "$parseDay",
                                                    color = if (isSelected) PrimaryGold else textColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                                                )
                                                if (dayEncontros.isNotEmpty()) {
                                                    // Little dots showing has meetings (Gold for sex, Blue for standard)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (dayEncontros.any { it.hadSex }) { if (dayEncontros.any { enc -> enc.hadSex && partnersById[enc.partnerId]?.isGP == true }) Color(0xFFE53935) else Color(0xFFFFEB3B) } else Color.White)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Show encounters matching selected day below
        val matchingEncs = remember(selectedDayStr, encountersByDate) {
            encountersByDate[selectedDayStr] ?: emptyList()
        }
        Text(
            text = "Ocorrência(s) de ${selectedDayStr.split("-").reversed().joinToString("/")}:",
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (matchingEncs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = "Nenhum histórico neste dia.", color = textVariantColor.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            } else {
                matchingEncs.forEach { encounter ->
                    val isGP = partnersById[encounter.partnerId]?.isGP == true
                    EncontroTimelineItem(
                        encontro = encounter,
                        mostrarGP = viewModel.mostrarGP,
                        isGP = isGP,
                        onDeleteClick = { viewModel.deleteEncontro(encounter) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EncontroTimelineItem(
    encontro: Encontro,
    mostrarGP: Boolean,
    isGP: Boolean = false,
    onDeleteClick: () -> Unit
) {
    val surfaceColor = Level2DarkSurface
    val primaryColor = PrimaryGold

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = encontro.partnerName,
                        color = OnSurfaceGold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = com.example.data.WomanCalculator.formatToBrazilianDate(encontro.date),
                        color = OnSurfaceGoldVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                // Show meeting cost or GP Service Charge
                if (mostrarGP || isGP) {
                    val costToShow = if (encontro.gpGastoValor > 0.0) encontro.gpGastoValor else encontro.motelCost
                    Text(
                        text = String.format("R$ %,.2f", costToShow),
                        color = primaryColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action tags list using FlowRow for responsive multi-line wrapping
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isGP) {
                    if (encontro.gpSexoVaginal) QuickBadge("🔞 Sexo Vaginal", Color(0xFF4CAF50))
                    if (encontro.gpOralSemCamisinha) QuickBadge("👄 Oral sem Camisinha", Color(0xFFE57373))
                    if (encontro.gpSexoAnal) QuickBadge("🍑 Sexo Anal", Color(0xFFFF9800))
                    if (encontro.gpBeijoNaBoca) QuickBadge("💋 Beijo na Boca", Color(0xFFE91E63))
                    if (encontro.gp69ComCamisinha) QuickBadge("🔄 69 com Camisinha", Color(0xFF26A69A))
                    if (encontro.gpMassagemErotica) QuickBadge("💆‍♀️ Massagem Erótica", Color(0xFFEC407A))
                    if (encontro.gpDominacaoBdsm) QuickBadge("⛓️ Dominação / BDSM", Color(0xFF7E57C2))
                    if (encontro.gpBeijoGrego) QuickBadge("🍯 Beijo Grego", Color(0xFFAB47BC))
                    if (encontro.gpInversaoFetiche) QuickBadge("🔄 Inversão", Color(0xFF26C6DA))
                    if (encontro.gpGargantaProfunda) QuickBadge("👅 Garganta Profunda", Color(0xFFFFA726))
                    if (encontro.gpFetichePe) QuickBadge("👣 Fetiche de Pé", Color(0xFF8D6E63))
                    if (encontro.gpFioTerra) QuickBadge("☝️ Fio Terra", Color(0xFF78909C))
                    if (!encontro.hadSex && !encontro.gpSexoVaginal && !encontro.gpSexoAnal && !encontro.gpOralSemCamisinha) {
                        QuickBadge("Apenas Conversa", Color.Gray)
                    }
                } else {
                    if (encontro.typeVaginal) QuickBadge("🔥 vaginal", Color(0xFF4CAF50))
                    if (encontro.typeOral) QuickBadge("😛 oral", Color(0xFFE57373))
                    if (encontro.typeAnal) QuickBadge("🍑 anal", Color(0xFFFF9800))
                    if (encontro.typeCreampie) QuickBadge("💦 🍕 creampie", primaryColor)
                    if (encontro.isPregnancy || encontro.isPregnancyMarked) QuickBadge("👶 Vou ser Papai!", Color(0xFF1E88E5))
                    if (encontro.isFirstEncontroSex) QuickBadge("⭐ Primeiro Sexo do Casal", Color(0xFFFFA726))
                    if (encontro.isVirginityLost) QuickBadge("🩸 Perda de Virgindade", Color(0xFFEF5350))
                    if (encontro.typeAnalCreampie) QuickBadge("🍑💦 anal creampie", Color(0xFFBA68C8))
                    if (!encontro.hadSex) QuickBadge("Apenas Conversa", Color.Gray)
                }
            }

            if (encontro.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = encontro.notes,
                    color = OnSurfaceGoldVariant.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDeleteClick) {
                    Text(text = "Excluir Encontro", color = Color.Red.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun QuickBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// ------------------- SCREEN 4: ADVANCED STATS MODULE (STATS) -------------------

@Composable
fun StatsCardScreen(viewModel: AgendaViewModel) {
    val activeFilter = viewModel.activeStatsFilter
    val partners by viewModel.allPartners.collectAsStateWithLifecycle()
    val encuentros by viewModel.allEncontros.collectAsStateWithLifecycle()

    val normalSexCount by viewModel.normalSexCount.collectAsStateWithLifecycle()
    val normalAnalCount by viewModel.normalAnalCount.collectAsStateWithLifecycle()
    val normalCreampieCount by viewModel.normalCreampieCount.collectAsStateWithLifecycle()
    val totalMotelCost by viewModel.totalMotelCost.collectAsStateWithLifecycle()

    val normalSexThisYear by viewModel.normalSexThisYear.collectAsStateWithLifecycle()
    val normalAnalThisYear by viewModel.normalAnalThisYear.collectAsStateWithLifecycle()
    val partnersThisYearState by viewModel.partnersThisYear.collectAsStateWithLifecycle()

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val encsByYear = remember(encuentros, partners) {
        val nonGpPartners = partners.filter { !it.isGP }
        val nonGpPartnerIds = nonGpPartners.map { it.id }.toSet()
        val nonGpEncs = encuentros.filter { nonGpPartnerIds.contains(it.partnerId) && it.hadSex }
        nonGpEncs.groupBy { enc ->
            try {
                val trimmed = enc.date.trim()
                if (trimmed.contains("/")) {
                    trimmed.split("/").last().toInt()
                } else if (trimmed.contains("-")) {
                    trimmed.split("-").first().toInt()
                } else {
                    currentYear
                }
            } catch (_: Exception) {
                currentYear
            }
        }
    }

    val currentYearEncs = encsByYear[currentYear] ?: emptyList()
    val sexThisYear = currentYearEncs.size
    val currentYearPartnerIds = currentYearEncs.map { it.partnerId }.toSet()
    val partnersCountThisYear = partners.count { !it.isGP && currentYearPartnerIds.contains(it.id) }
    val analThisYear = currentYearEncs.sumOf { enc -> if (enc.typeAnal) maxOf(1, enc.countAnal) else 0 }
    val vaginalThisYear = currentYearEncs.sumOf { enc -> if (enc.typeVaginal) maxOf(1, enc.countVaginal) else 0 }

    val bestYearEntry = encsByYear.maxByOrNull { it.value.size }
    val bestYear = bestYearEntry?.key ?: currentYear
    val bestYearEncs = bestYearEntry?.value ?: emptyList()
    val bestYearSex = bestYearEncs.size
    val bestYearPartnerIds = bestYearEncs.map { it.partnerId }.toSet()
    val bestYearPartners = partners.count { !it.isGP && bestYearPartnerIds.contains(it.id) }
    val bestYearAnal = bestYearEncs.sumOf { enc -> if (enc.typeAnal) maxOf(1, enc.countAnal) else 0 }

    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
    val cardColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP TAB CHIPS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsFilter.values().forEach { filter ->
                val isSelected = activeFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (isSelected) PrimaryGold.copy(alpha = 0.15f) else cardColor)
                        .border(
                            1.dp,
                            if (isSelected) PrimaryGold else Color.LightGray.copy(alpha = 0.15f),
                            RoundedCornerShape(100.dp)
                        )
                        .clickable { viewModel.activeStatsFilter = filter }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) PrimaryGold else textVariantColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        when (activeFilter) {
            StatsFilter.GERAL -> {
                // Large summary row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "Gastos Motéis", color = textVariantColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = String.format("R$ %,.0f", totalMotelCost), color = PrimaryGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "Total Encontros", color = textVariantColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${encuentros.size}", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Progress Bars details
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Detalhamento Práticas", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                        val oral = encuentros.count { it.typeOral }
                        val anal = encuentros.count { it.typeAnal }
                        val vaginal = encuentros.count { it.typeVaginal }
                        val creampie = encuentros.count { it.typeCreampie }
                        val analCreampie = encuentros.count { it.typeAnalCreampie }
                        val totalEncs = encuentros.size.coerceAtLeast(1)

                        StatProgressRow(label = "Vaginal", count = vaginal, maxCount = totalEncs)
                        StatProgressRow(label = "Oral", count = oral, maxCount = totalEncs)
                        StatProgressRow(label = "Anal", count = anal, maxCount = totalEncs)
                        StatProgressRow(label = "Creampies", count = creampie, maxCount = totalEncs)
                        StatProgressRow(label = "Anal Creampie", count = analCreampie, maxCount = totalEncs)
                    }
                }
            }

            StatsFilter.ANUAL -> {
                // Card 1: Temporada Atual (Ano Atual)
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Temporada Atual ($currentYear)", color = PrimaryGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Surface(
                                color = PrimaryGold.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Ano Vigente",
                                    color = PrimaryGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Divider(color = Color.LightGray.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Sexo ($currentYear)", color = textVariantColor, fontSize = 12.sp)
                                Text(text = "$sexThisYear", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                            Column {
                                Text(text = "Parceiras (Ano)", color = textVariantColor, fontSize = 12.sp)
                                Text(text = "$partnersCountThisYear", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Com Anal (Ano)", color = textVariantColor, fontSize = 12.sp)
                                Text(text = "$analThisYear", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                            Column {
                                Text(text = "Vaginal (Ano)", color = textVariantColor, fontSize = 12.sp)
                                Text(text = "$vaginalThisYear", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                // Card 2: Melhor Ano (Ano Recorde)
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, PrimaryGold.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🏆 Melhor Ano (Recorde Histórico)", color = PrimaryGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Surface(
                                color = PrimaryGold,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Ano: $bestYear",
                                    color = OnPrimaryGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Divider(color = Color.LightGray.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Maior Quantidade de Sexo", color = textVariantColor, fontSize = 12.sp)
                                Text(text = "$bestYearSex", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                            Column {
                                Text(text = "Parceiras no Recorde", color = textVariantColor, fontSize = 12.sp)
                                Text(text = "$bestYearPartners", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Com Anal no Ano Recorde", color = textVariantColor, fontSize = 12.sp)
                                Text(text = "$bestYearAnal", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            StatsFilter.RANKINGS -> {
                // Top 5 standard women
                val top5 = remember(partners) {
                    partners.filter { !it.isGP }.sortedByDescending { it.rating }.take(5)
                }

                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Top 5 Mulheres (Melhor Nota)", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                        if (top5.isEmpty()) {
                            Text(text = "Nenhuma mulher qualificada.", color = textVariantColor, fontSize = 12.sp)
                        } else {
                            top5.forEachIndexed { idx, p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(text = "#${idx+1}", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = p.name, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    Text(
                                        text = String.format("%.1f ★", p.rating),
                                        color = PrimaryGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Channel Efficiency ( Tinder, Instagram, etc)
                val channelsEnum = remember(partners, encuentros) {
                    val list = mutableListOf<ChannelStat>()
                    
                    val origins = partners.filter { !it.isGP }.map { it.origin }.distinct()
                    origins.forEach { orig ->
                        val origPartners = partners.filter { !it.isGP && it.origin == orig }
                        val converted = origPartners.count { p ->
                            encuentros.any { it.partnerId == p.id && it.hadSex && (it.typeOral || it.typeAnal || it.typeVaginal) }
                        }
                        val pct = if (origPartners.isNotEmpty()) {
                            (converted.toDouble() / origPartners.size.toDouble()) * 100.0
                        } else 0.0
                        list.add(ChannelStat(orig, origPartners.size, converted, pct))
                    }
                    list.sortedByDescending { it.percent }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Eficiência de Canais (Origem)", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                        channelsEnum.forEach { ch ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = ch.channel, color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "Registrados: ${ch.total} | Transadas: ${ch.sexCount}", color = textVariantColor, fontSize = 11.sp)
                                }
                                Text(
                                    text = String.format("%.1f%%", ch.percent),
                                    color = PrimaryGold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

data class ChannelStat(val channel: String, val total: Int, val sexCount: Int, val percent: Double)

@Composable
fun StatProgressRow(
    label: String,
    count: Int,
    maxCount: Int
) {
    val pct = if (maxCount > 0) count.toFloat() / maxCount.toFloat() else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = OnSurfaceGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "$count ocor.", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = PrimaryGold,
            trackColor = Level2DarkSurface
        )
    }
}

// ------------------- SCREEN 5: CONFIGURAÇÕES COMPLETA (CONFIG) -------------------

@Composable
fun SettingsScreen(viewModel: AgendaViewModel) {
    val context = LocalContext.current

    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
    val cardColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White
    val outlineBorderColor = if (viewModel.isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.12f)
    val dividerColor = if (viewModel.isDarkTheme) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.12f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Estatísticas Link Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, PrimaryGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clickable { viewModel.currentTab = AppTab.STATS },
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(24.dp))
                    Column {
                        Text(text = "Estatísticas Detalhadas H", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Ver taxas de conversão, gráficos, rankings e mais.", color = textVariantColor, fontSize = 10.sp)
                    }
                }
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryGold)
            }
        }

        // Visual theme
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, outlineBorderColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Visual", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Tema Escuro Champagne", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Alterna o visual para diminuir reflexos.", color = textVariantColor, fontSize = 11.sp)
                    }
                    Switch(
                        checked = viewModel.isDarkTheme,
                        onCheckedChange = { viewModel.setDarkThemeEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = OnPrimaryGold, checkedTrackColor = PrimaryGold)
                    )
                }
            }
        }

        // Hide / visibility
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, outlineBorderColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Privacidade Geral", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Habilitar Gestão Sensível GP", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Exibe dados de GPs e tarifas no dashboard e abas.", color = textVariantColor, fontSize = 11.sp)
                    }
                    Switch(
                        checked = viewModel.mostrarGP,
                        onCheckedChange = { viewModel.setMostrarGPValue(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = OnPrimaryGold, checkedTrackColor = PrimaryGold)
                    )
                }
            }
        }

        // Treinador IA
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, outlineBorderColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Treinador IA (Coach)", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Popup Flutuante do Coach H", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Habilita o botão redondo flutuante na tela para simular prints e enviar ao Coach IA.", color = textVariantColor, fontSize = 11.sp)
                    }
                    Switch(
                        checked = viewModel.isFloatingCoachBubbleEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Settings.canDrawOverlays(context)) {
                                    viewModel.updateFloatingCoachBubble(true)
                                    val serviceIntent = Intent(context, com.example.FloatingCoachService::class.java)
                                    try {
                                        context.startService(serviceIntent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    Toast.makeText(context, "Por favor, autorize a sobreposição de tela nas configurações!", Toast.LENGTH_LONG).show()
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                        context.startActivity(intent)
                                    }
                                }
                            } else {
                                viewModel.updateFloatingCoachBubble(false)
                                val serviceIntent = Intent(context, com.example.FloatingCoachService::class.java)
                                try {
                                    context.stopService(serviceIntent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = OnPrimaryGold, checkedTrackColor = PrimaryGold)
                    )
                }
            }
        }

        // Acceso lock settings
        var pinInput by remember { mutableStateOf(viewModel.pinCode) }

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, outlineBorderColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Acesso de Segurança", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            pinInput = input
                        }
                    },
                    label = { Text("Senha PIN (4 dígitos)") },
                    placeholder = { Text("Ex: 1234") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = PrimaryGold
                    )
                )

                Button(
                    onClick = {
                        if (pinInput.length == 4 || pinInput.isEmpty()) {
                            viewModel.setPinCodeValue(pinInput)
                            Toast.makeText(context, if (pinInput.isEmpty()) "Segurança desativada." else "Senha PIN salva com sucesso!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Insira um PIN completo de 4 números!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Salvar PIN")
                }

                Divider(color = dividerColor)

                // Biometrics toggle (only works if PIN exists!)
                val isPinConfigured = viewModel.pinCode.isNotEmpty()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Abertura por Digital",
                            color = if (isPinConfigured) textColor else textColor.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPinConfigured) "Desbloquear o app com o scanner de digital." else "Cadastre um PIN acima antes.",
                            color = textVariantColor,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = viewModel.isBiometricsEnabled,
                        onCheckedChange = { enabled ->
                            if (isPinConfigured) {
                                viewModel.setBiometricsEnabledValue(enabled)
                            }
                        },
                        enabled = isPinConfigured,
                        colors = SwitchDefaults.colors(checkedThumbColor = OnPrimaryGold, checkedTrackColor = PrimaryGold)
                    )
                }

                if (isPinConfigured) {
                    Button(
                        onClick = { viewModel.isAppLocked = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f), contentColor = textColor),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "BLOQUEAR AGORA")
                    }
                }
            }
        }

        // BACKUP DATA MANAGERS WITH SYSTEM DIALOGS - SYSTEM SPEC 21
        var showPasteBackupDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current

        val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let { viewModel.exportBackupToUri(context, it) }
        }

        val openDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let { viewModel.importBackupFromUri(context, it) }
        }

        val jsonFilePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { viewModel.importBackupFromUri(context, it) }
        }

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, outlineBorderColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Backup e Sessão Local", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                Button(
                    onClick = {
                        try {
                            jsonFilePickerLauncher.launch("application/json")
                        } catch (e: Exception) {
                            try {
                                openDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Erro ao abrir seletor de arquivos.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("import_backup_json_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold)
                ) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Importar JSON", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Importar Backup (JSON)", fontWeight = FontWeight.Bold)
                }

                // Dedicated folder "Agenda H" options
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGold.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Pasta Interna \"Agenda H\"", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = "Grave e recupere backups com os dados de parceiras e encontros diretamente na pasta 'Agenda H' do seu celular.",
                            color = textVariantColor,
                            fontSize = 11.sp
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.saveBackupToAgendaH(context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold.copy(alpha = 0.25f), contentColor = textColor)
                            ) {
                                Text("Gravar na Agenda H", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { viewModel.restoreBackupFromAgendaH(context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold)
                            ) {
                                Text("Recuperar da Agenda H", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                SettingsClickableRow(
                    icon = Icons.Default.Save,
                    title = "Exportar arquivo para o Celular",
                    sub = "Escolha uma pasta do sistema de arquivos para salvar seu backup (.json).",
                    onClick = {
                        try {
                            createDocumentLauncher.launch("agenda_h_backup_${System.currentTimeMillis()}.json")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao abrir salvador do sistema.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                SettingsClickableRow(
                    icon = Icons.Default.FolderOpen,
                    title = "Importar arquivo do Celular (TXT / JSON)",
                    sub = "Abra seu arquivo de backup (.txt ou .json) salvo no armazenamento local.",
                    onClick = {
                        try {
                            openDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao abrir leitor do sistema.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                SettingsClickableRow(
                    icon = Icons.Default.ContentPaste,
                    title = "Colar JSON do Claude / HTML Web",
                    sub = "Cole texto formatado do Claude para migrar os dados no aplicativo de forma instantânea.",
                    onClick = { showPasteBackupDialog = true }
                )

                SettingsClickableRow(
                    icon = Icons.Default.CloudDownload,
                    title = "Exportar Cache Temporário",
                    sub = "Salva em local temporário cache do celular.",
                    onClick = { viewModel.exportBackup() }
                )

                SettingsClickableRow(
                    icon = Icons.Default.CloudUpload,
                    title = "Restaurar Cache Temporário",
                    sub = "Lê arquivo temporário salvo no cache.",
                    onClick = { viewModel.restoreBackup() }
                )

                SettingsClickableRow(
                    icon = Icons.Default.Casino,
                    title = "Gerar Dados de Demonstração (Mock)",
                    sub = "Re-popula o banco de dados local com parceiras e encontros de exemplo de forma instantânea.",
                    onClick = { viewModel.regeneratePredefinedMockData() }
                )

                Button(
                    onClick = { viewModel.wipeAllData() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Text(text = "APAGAR TODO O HISTÓRICO", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showPasteBackupDialog) {
            Dialog(onDismissRequest = { showPasteBackupDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(1.dp, PrimaryGold)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Migrar Dados (Claude HTML / JSON)",
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cole o conteúdo JSON gerado pelo chat do Claude ou web-app para migrar. O sistema irá traduzir todos os campos automaticamente.",
                            color = textVariantColor,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        var textInputState by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = textInputState,
                            onValueChange = { textInputState = it },
                            placeholder = { Text("Cole aqui seu JSON...", fontSize = 11.sp, color = textVariantColor.copy(alpha = 0.5f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = PrimaryGold
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showPasteBackupDialog = false }) {
                                Text("Cancelar", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (textInputState.isNotBlank()) {
                                        viewModel.importBackupFromText(context, textInputState) {
                                            showPasteBackupDialog = false
                                        }
                                    } else {
                                        Toast.makeText(context, "Por favor, cole algum texto primeiro.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold)
                            ) {
                                Text("Importar")
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(24.dp))
        Column {
            Text(text = title, color = OnSurfaceGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = sub, color = OnSurfaceGoldVariant.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

// ------------------- SCREEN 6: GAROTAS DE PROGRAMA (GPS) -------------------

@Composable
fun GPListScreen(viewModel: AgendaViewModel) {
    val gpsState by viewModel.allPartners.collectAsStateWithLifecycle(initialValue = emptyList())
    var activeDetailGP by remember { mutableStateOf<Partner?>(null) }
    var partnerToDelete by remember { mutableStateOf<Partner?>(null) }
    var gpForNewEncontro by remember { mutableStateOf<Partner?>(null) }

    if (activeDetailGP != null) {
        val updatedGP = gpsState.find { it.id == activeDetailGP!!.id } ?: activeDetailGP
        if (updatedGP != null) {
            GPDetailScreen(
                gp = updatedGP,
                onBack = { activeDetailGP = null },
                viewModel = viewModel
            )
        } else {
            activeDetailGP = null
        }
    } else {
        val search = viewModel.gpSearchQuery
        val gps by viewModel.filteredGPs.collectAsStateWithLifecycle()
        val encuentros by viewModel.allEncontros.collectAsStateWithLifecycle()

        val encountersByPartner = remember(encuentros) {
            encuentros.groupBy { it.partnerId }
        }

        // Ordenar os cards das mulheres com encontros sexuais mais recentes do mais recente até o mais antigo
        val sortedGps = remember(gps, encountersByPartner, search) {
            gps.filter { gp ->
                search.isEmpty() ||
                gp.name.contains(search, ignoreCase = true) ||
                gp.address.contains(search, ignoreCase = true) ||
                gp.location.contains(search, ignoreCase = true) ||
                gp.notes.contains(search, ignoreCase = true)
            }.sortedByDescending { gp ->
                val gpEncs = encountersByPartner[gp.id] ?: emptyList()
                val latestEncMillis = gpEncs.filter { it.hadSex || it.gpSexoVaginal || it.gpSexoAnal || it.gpOralSemCamisinha || it.gpGastoValor > 0 }
                    .map { com.example.data.WomanCalculator.parseDateToMillis(it.date) }
                    .maxOrNull() ?: 0L
                if (latestEncMillis > 0L) latestEncMillis else gp.createdAt
            }
        }

        val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
        val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
        val cardColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White
        val context = LocalContext.current

        val totalGpsRegistradas = gpsState.count { it.isGP }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "GP — $totalGpsRegistradas CADASTRADAS",
                color = textVariantColor.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )

            // CARD DE CADASTRO UNIFICADO
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.gpToEdit = null
                        viewModel.showAddGPDialog = true
                    },
                colors = CardDefaults.cardColors(containerColor = PrimaryGold.copy(alpha = 0.12f)),
                border = BorderStroke(1.5.dp, PrimaryGold),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(PrimaryGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = OnPrimaryGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Cadastrar Nova GP 💃",
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cadastro com encontro sexual automático",
                                color = textVariantColor,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(
                        text = "CADASTRAR",
                        color = PrimaryGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // BUSCA DE GPS
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.gpSearchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardColor, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text(text = "Buscar por nome, local ou fetiche...", color = textVariantColor.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PrimaryGold) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.15f)
                ),
                singleLine = true
            )

            // LISTA DE CARDS DE GPS ORDENADOS POR ENCONTRO MAIS RECENTE
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (sortedGps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (search.isEmpty()) "Nenhuma GP cadastrada ainda.\nToque no card acima para cadastrar!" else "Nenhuma GP encontrada para a busca.",
                                color = textVariantColor.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(sortedGps, key = { it.id }) { gp ->
                        val gpEncontros = remember(gp.id, encountersByPartner) {
                            encountersByPartner[gp.id] ?: emptyList()
                        }

                        val isMostRecentOverall = sortedGps.firstOrNull()?.id == gp.id && gpEncontros.any { it.hadSex || it.gpSexoVaginal || it.gpSexoAnal || it.gpOralSemCamisinha }

                        val totalCount = gpEncontros.size
                        val analCount = remember(gpEncontros) {
                            gpEncontros.sumOf { maxOf(it.countAnal, if (it.gpSexoAnal || it.typeAnal) 1 else 0) }
                        }
                        val oralCount = remember(gpEncontros) {
                            gpEncontros.sumOf { maxOf(it.countOral, if (it.gpOralSemCamisinha || it.typeOral) 1 else 0) }
                        }
                        val hasAnal = analCount > 0
                        val hasOral = oralCount > 0

                        val lastDateBr = remember(gpEncontros) {
                            val latestEnc = gpEncontros.sortedByDescending { com.example.data.WomanCalculator.parseDateToMillis(it.date) }.firstOrNull()
                            if (latestEnc != null) {
                                try {
                                    val parts = latestEnc.date.split("-")
                                    if (parts.size == 3 && parts[0].length == 4) "${parts[2]}/${parts[1]}/${parts[0]}" else latestEnc.date
                                } catch (e: Exception) {
                                    latestEnc.date
                                }
                            } else {
                                "Sem encontros"
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isMostRecentOverall) PrimaryGold.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Top row: Nome, Badge mais recente, Ações de editar e excluir
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = gp.name,
                                            color = textColor,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isMostRecentOverall) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(PrimaryGold)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "🔥 MAIS RECENTE",
                                                    color = Color.Black,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = {
                                                viewModel.gpToEdit = gp
                                                viewModel.showAddGPDialog = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text(text = "✏️", fontSize = 14.sp)
                                        }
                                        IconButton(
                                            onClick = { partnerToDelete = gp },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Excluir GP",
                                                tint = Color.Red.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Endereço & WhatsApp
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = "📍", fontSize = 12.sp)
                                        val rawAddr = gp.address.ifEmpty { "Sem endereço" }
                                        val lastComma = rawAddr.lastIndexOf(',')
                                        val cityToShow = if (lastComma != -1) rawAddr.substring(lastComma + 1).trim() else rawAddr
                                        Text(
                                            text = if (rawAddr != cityToShow) "$rawAddr (Cidade: $cityToShow)" else rawAddr,
                                            color = textVariantColor.copy(alpha = 0.9f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (gp.phone.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF25D366))
                                                .clickable {
                                                    try {
                                                        val cleanPhone = gp.phone.replace("[^0-9]".toRegex(), "")
                                                        val finalPhone = if (cleanPhone.length <= 11) "55$cleanPhone" else cleanPhone
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$finalPhone"))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "WhatsApp indisponível.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(text = "💬", fontSize = 11.sp)
                                                Text(text = "WhatsApp", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Preço & Observações
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (gp.gpPrice > 0) {
                                        Text(
                                            text = "💰 Tarifa: R$ ${gp.gpPrice.toInt()}",
                                            color = Color(0xFF66BB6A),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "${totalCount}x encontros | ${analCount}x anal",
                                        color = textVariantColor.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (gp.notes.isNotEmpty()) {
                                    Text(
                                        text = "“${gp.notes}”",
                                        color = textVariantColor.copy(alpha = 0.75f),
                                        fontSize = 11.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Badges de práticas e data do último encontro
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Custom Anal Check indicator
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (hasAnal) Color(0xFF4CAF50) else Color.Transparent)
                                                .border(
                                                    1.dp,
                                                    if (hasAnal) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f),
                                                    RoundedCornerShape(3.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasAnal) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Anal",
                                            color = if (hasAnal) Color(0xFF4CAF50) else textVariantColor.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Custom Oral s/camisinha Check indicator
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (hasOral) Color(0xFF4CAF50) else Color.Transparent)
                                                .border(
                                                    1.dp,
                                                    if (hasOral) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f),
                                                    RoundedCornerShape(3.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasOral) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Oral s/camisinha",
                                            color = if (hasOral) Color(0xFF4CAF50) else textVariantColor.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Text(
                                        text = "Último: $lastDateBr",
                                        color = PrimaryGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)

                                // DOIS BOTÕES DE AÇÃO NO CARD DA GP:
                                // 1. "+ Registrar Encontro" (permite registrar mais encontros em datas diferentes)
                                // 2. "Ver Histórico"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { gpForNewEncontro = gp },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.2f).height(40.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Black)
                                            Text(text = "Novo Encontro 🔞", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { activeDetailGP = gp },
                                        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Text(text = "Ver Histórico", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Dialog para registrar mais encontros sexuais em datas diferentes no mesmo card da GP
        if (gpForNewEncontro != null) {
            AddGPEncontroDialog(
                gp = gpForNewEncontro!!,
                viewModel = viewModel,
                onDismiss = { gpForNewEncontro = null }
            )
        }

        if (partnerToDelete != null) {
            AlertDialog(
                onDismissRequest = { partnerToDelete = null },
                title = { Text(text = "Excluir Cadastro", fontWeight = FontWeight.Bold, color = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)) },
                text = { Text(text = "Tem certeza que deseja excluir o cadastro de ${partnerToDelete?.name}? Essa ação não poderá ser desfeita.", color = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)) },
                confirmButton = {
                    Button(
                        onClick = {
                            partnerToDelete?.let {
                                viewModel.deletePartner(it)
                            }
                            partnerToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { partnerToDelete = null }) {
                        Text("Cancelar", color = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B))
                    }
                },
                containerColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White,
                tonalElevation = 6.dp
            )
        }
    }
}

@Composable
fun GPDetailScreen(
    gp: Partner,
    onBack: () -> Unit,
    viewModel: AgendaViewModel
) {
    val encuentros by viewModel.allEncontros.collectAsStateWithLifecycle()
    val gpEncs = remember(encuentros, gp.id) { encuentros.filter { it.partnerId == gp.id } }

    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
    val cardBg = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White
    val context = LocalContext.current

    // For editing a GP encounter
    var encounterToEdit by remember { mutableStateOf<Encontro?>(null) }
    var showGPEncontroDialog by remember { mutableStateOf(false) }

    if (showGPEncontroDialog) {
        AddGPEncontroDialog(
            gp = gp,
            viewModel = viewModel,
            onDismiss = { showGPEncontroDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Voltar Button
        Row(
            modifier = Modifier
                .clickable { onBack() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← Voltar",
                color = PrimaryGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // GP Identity Card
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = gp.name,
                color = textColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            val rawAddr = gp.address.ifEmpty { "Sem endereço" }
            val lastComma = rawAddr.lastIndexOf(',')
            val cityToShow = if (lastComma != -1) rawAddr.substring(lastComma + 1).trim() else rawAddr
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "📍", fontSize = 14.sp)
                Text(
                    text = if (rawAddr != cityToShow) "$rawAddr (Cidade: $cityToShow)" else rawAddr,
                    color = PrimaryGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (gp.phone.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        try {
                            val cleanPhone = gp.phone.replace("[^0-9]".toRegex(), "")
                            val finalPhone = if (cleanPhone.length <= 11) "55$cleanPhone" else cleanPhone
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$finalPhone"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp indisponível.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "💬", fontSize = 16.sp)
                        Text(
                            text = "WhatsApp: ${gp.phone}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (gp.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = gp.notes,
                    color = textVariantColor.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }

        // 4 Stat boxes
        val totalEncsCount = gpEncs.size
        val idxAnalCount = gpEncs.sumOf { if (it.gpSexoAnal) 1 else 0 }
        val idxOralCount = gpEncs.sumOf { if (it.gpOralSemCamisinha) 1 else 0 }
        val gpSpentTotalCalculated = gpEncs.sumOf { it.motelCost }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Box 1: Encontros
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .background(cardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$totalEncsCount", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "ENCONTROS", color = textVariantColor.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Box 2: Anal
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .background(cardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$idxAnalCount", color = PrimaryGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "ANAL", color = textVariantColor.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Box 3: Oral
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .background(cardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$idxOralCount", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "ORAL", color = textVariantColor.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Box 4: Gasto
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .background(cardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "R$ ${gpSpentTotalCalculated.toInt()}", color = Color(0xFFEF5350), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "GASTO", color = textVariantColor.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Two Big Buttons: Registrar and Editar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Button 1: Registrar (Border only)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, PrimaryGold, RoundedCornerShape(12.dp))
                    .clickable {
                        showGPEncontroDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(text = "+", color = PrimaryGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(text = "Registrar Encontro", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Button 2: Editar (Gray Filled)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.gpToEdit = gp
                        viewModel.showAddGPDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(text = "✏️", color = textColor, fontSize = 16.sp)
                    Text(text = "Editar", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Divider & Title
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "HISTÓRICO DE ENCONTROS",
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
        }

        // Meetings list
        if (gpEncs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Nenhum histórico de encontros registrado.", color = textVariantColor.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                gpEncs.sortedByDescending { com.example.data.WomanCalculator.parseDateToMillis(it.date) }.forEach { encontro ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Left gold vertical bar accent line
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(76.dp)
                                    .background(PrimaryGold)
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val dateBr = com.example.data.WomanCalculator.formatToBrazilianDate(encontro.date)
                                    Text(
                                        text = "📅 $dateBr   💰 R$ ${encontro.motelCost.toInt()}",
                                        color = textColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Edit button with golden yellow border
                                        Box(
                                            modifier = Modifier
                                                .size(width = 38.dp, height = 28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(1.dp, PrimaryGold.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                .clickable { encounterToEdit = encontro },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "✏️", fontSize = 11.sp)
                                        }
                                        
                                        // Delete button with red border
                                        Box(
                                            modifier = Modifier
                                                .size(width = 38.dp, height = 28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    viewModel.deleteEncontro(encontro)
                                                    Toast.makeText(context, "Encontro removido!", Toast.LENGTH_SHORT).show()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "🗑️", fontSize = 11.sp)
                                        }
                                    }
                                }

                                if (encontro.notes.isNotEmpty()) {
                                    Text(text = encontro.notes, color = textVariantColor, fontSize = 12.sp)
                                }

                                // Interactive Services Badges FlowRow with counts
                                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    if (encontro.gpSexoVaginal) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(PrimaryGold.copy(alpha = 0.12f))
                                                .border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("🔞 Sexo Vaginal", color = textColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (encontro.gpOralSemCamisinha) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE57373).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFFE57373).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("👄 Oral sem Camisinha", color = Color(0xFFE57373), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (encontro.gpSexoAnal) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFF9800).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("🍑 Sexo Anal", color = Color(0xFFFF9800), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (encontro.gpBeijoNaBoca) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE91E63).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFFE91E63).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("💋 Beijo na Boca", color = Color(0xFFE91E63), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (encontro.gp69ComCamisinha) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF26A69A).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFF26A69A).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("🔄 69 com Camisinha", color = Color(0xFF26A69A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (encontro.gpMassagemErotica) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFEC407A).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFFEC407A).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("💆‍♀️ Massagem Erótica", color = Color(0xFFEC407A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (encontro.gpDominacaoBdsm) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF7E57C2).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFF7E57C2).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("⛓️ Dominação / BDSM", color = Color(0xFF7E57C2), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (encontro.gpBeijoGrego) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFAB47BC).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFFAB47BC).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("🍯 Beijo Grego", color = Color(0xFFAB47BC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (encontro.gpInversaoFetiche) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF26C6DA).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFF26C6DA).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("🔄 Inversão", color = Color(0xFF26C6DA), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (encontro.gpGargantaProfunda) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFFA726).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFFFFA726).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("👅 Garganta Profunda", color = Color(0xFFFFA726), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (encontro.gpFetichePe) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF8D6E63).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFF8D6E63).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("👣 Fetiche de Pé", color = Color(0xFF8D6E63), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (encontro.gpFioTerra) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF78909C).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFF78909C).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("☝️ Fio Terra", color = Color(0xFF78909C), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }

    // Encounter Edit dialog overlay inside GPDetailScreen
    if (encounterToEdit != null) {
        EditEncontroDialog(
            encontro = encounterToEdit!!,
            onDismiss = { encounterToEdit = null },
            onConfirm = { updated ->
                viewModel.updateEncontro(updated)
                encounterToEdit = null
                Toast.makeText(context, "Encontro atualizado com sucesso!", Toast.LENGTH_SHORT).show()
            },
            isGP = true,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEncontroDialog(
    encontro: Encontro,
    onDismiss: () -> Unit,
    onConfirm: (Encontro) -> Unit,
    isGP: Boolean,
    viewModel: AgendaViewModel
) {
    var date by remember { mutableStateOf(encontro.date) }
    var time by remember { mutableStateOf(encontro.time) }
    var costStr by remember { mutableStateOf(encontro.motelCost.toString()) }
    
    var typeOral by remember { mutableStateOf(if (isGP) encontro.gpOralSemCamisinha else encontro.typeOral) }
    var countOral by remember { mutableStateOf(if (encontro.countOral > 0) encontro.countOral else if (if (isGP) encontro.gpOralSemCamisinha else encontro.typeOral) 1 else 0) }
    var typeAnal by remember { mutableStateOf(if (isGP) encontro.gpSexoAnal else encontro.typeAnal) }
    var countAnal by remember { mutableStateOf(if (encontro.countAnal > 0) encontro.countAnal else if (if (isGP) encontro.gpSexoAnal else encontro.typeAnal) 1 else 0) }
    var typeVaginal by remember { mutableStateOf(if (isGP) encontro.gpSexoVaginal else encontro.typeVaginal) }
    var countVaginal by remember { mutableStateOf(if (encontro.countVaginal > 0) encontro.countVaginal else if (if (isGP) encontro.gpSexoVaginal else encontro.typeVaginal) 1 else 0) }
    var typeCreampie by remember { mutableStateOf(if (isGP) encontro.gpBeijoNaBoca else encontro.typeCreampie) }
    var countCreampie by remember { mutableStateOf(if (encontro.countCreampie > 0) encontro.countCreampie else if (if (isGP) encontro.gpBeijoNaBoca else encontro.typeCreampie) 1 else 0) }
    var typeAnalCreampie by remember { mutableStateOf(encontro.typeAnalCreampie) }
    var countAnalCreampie by remember { mutableStateOf(if (encontro.countAnalCreampie > 0) encontro.countAnalCreampie else if (encontro.typeAnalCreampie) 1 else 0) }
    
    var typeFacial by remember { mutableStateOf(if (isGP) encontro.gp69ComCamisinha else encontro.typeFacial) }
    var countFacial by remember { mutableStateOf(if (encontro.countFacial > 0) encontro.countFacial else if (if (isGP) encontro.gp69ComCamisinha else encontro.typeFacial) 1 else 0) }
    var typeSquirt by remember { mutableStateOf(if (isGP) encontro.gpMassagemErotica else encontro.typeSquirt) }
    var countSquirt by remember { mutableStateOf(if (encontro.countSquirt > 0) encontro.countSquirt else if (if (isGP) encontro.gpMassagemErotica else encontro.typeSquirt) 1 else 0) }
    var typeDeepthroat by remember { mutableStateOf(if (isGP) encontro.gpDominacaoBdsm else encontro.typeDeepthroat) }
    var countDeepthroat by remember { mutableStateOf(if (encontro.countDeepthroat > 0) encontro.countDeepthroat else if (if (isGP) encontro.gpDominacaoBdsm else encontro.typeDeepthroat) 1 else 0) }

    var gpBeijoGrego by remember { mutableStateOf(encontro.gpBeijoGrego) }
    var gpInversaoFetiche by remember { mutableStateOf(encontro.gpInversaoFetiche) }
    var gpGargantaProfunda by remember { mutableStateOf(encontro.gpGargantaProfunda) }
    var gpFetichePe by remember { mutableStateOf(encontro.gpFetichePe) }
    var gpFioTerra by remember { mutableStateOf(encontro.gpFioTerra) }
    
    var notes by remember { mutableStateOf(encontro.notes) }
    var hadSex by remember { mutableStateOf(encontro.hadSex) }
    var ratingStr by remember { mutableStateOf(encontro.rating.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        val dlgBgColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White
        val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
        val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
        val dlgBorderStroke = BorderStroke(1.dp, if (viewModel.isDarkTheme) PrimaryGold.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 14.dp),
            shape = RoundedCornerShape(16.dp),
            color = dlgBgColor,
            border = dlgBorderStroke
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Editar Encontro",
                    color = PrimaryGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                val editCalContext = LocalContext.current
                val displayEditDate = com.example.data.WomanCalculator.formatToBrazilianDate(date)
                val showDatePicker = {
                    val calendar = Calendar.getInstance()
                    if (date.contains("-")) {
                        val parts = date.split("-")
                        if (parts.size == 3) {
                            val y = parts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                            val m = (parts[1].toIntOrNull() ?: 1) - 1
                            val d = parts[2].toIntOrNull() ?: 1
                            calendar.set(y, m, d)
                        }
                    } else if (date.contains("/")) {
                        val parts = date.split("/")
                        if (parts.size == 3) {
                            val d = parts[0].toIntOrNull() ?: 1
                            val m = (parts[1].toIntOrNull() ?: 1) - 1
                            val y = parts[2].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                            calendar.set(y, m, d)
                        }
                    }
                    android.app.DatePickerDialog(
                        editCalContext,
                        { _, year, month, dayOfMonth ->
                            date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker() }
                ) {
                    OutlinedTextField(
                        value = displayEditDate,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Data do encontro (Toque para alterar)", color = textVariantColor) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker() }) {
                                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Selecionar Data", tint = PrimaryGold)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            focusedBorderColor = PrimaryGold,
                            unfocusedTextColor = textColor,
                            unfocusedBorderColor = textVariantColor.copy(alpha = 0.5f),
                            disabledTextColor = textColor,
                            disabledBorderColor = textVariantColor.copy(alpha = 0.5f),
                            disabledLabelColor = textVariantColor
                        )
                    )
                }

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text(if (isGP) "Tarifa GP Pago (R$)" else "Gasto em Motel (R$)", color = textVariantColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, focusedBorderColor = PrimaryGold, unfocusedTextColor = textColor, unfocusedBorderColor = textVariantColor.copy(alpha = 0.5f))
                )

                if (!isGP) {
                    OutlinedTextField(
                        value = ratingStr,
                        onValueChange = { ratingStr = it },
                        label = { Text("Nota do Encontro (0.0 - 10.0)", color = textVariantColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, focusedBorderColor = PrimaryGold, unfocusedTextColor = textColor, unfocusedBorderColor = textVariantColor.copy(alpha = 0.5f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Rolou Prática de Sexo ?", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = hadSex,
                        onCheckedChange = { hadSex = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = OnPrimaryGold, checkedTrackColor = PrimaryGold)
                    )
                }

                if (hadSex) {
                    Text(text = "Repetíveis (Toque para marcar e ajustar quant.):", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FetishRow(
                            label = if (isGP) "🔥 Sexo vaginal" else "🔥 Sexo vaginal",
                            emoji = "🔥",
                            subtitleChecked = if (isGP) "Vaginal" else "Estreante",
                            checked = typeVaginal,
                            onCheckedChange = { checked ->
                                typeVaginal = checked
                                if (checked && countVaginal == 0) countVaginal = 1
                            },
                            count = countVaginal,
                            onCountChange = { countVaginal = it }
                        )

                        FetishRow(
                            label = if (isGP) "🍑 Sexo anal" else "🍑 Sexo anal",
                            emoji = "🍑",
                            subtitleChecked = if (isGP) "Anal" else "Exploradora",
                            checked = typeAnal,
                            onCheckedChange = { checked ->
                                typeAnal = checked
                                if (checked && countAnal == 0) countAnal = 1
                            },
                            count = countAnal,
                            onCountChange = { countAnal = it }
                        )

                        FetishRow(
                            label = if (isGP) "💋 Beijos na boca" else "💦 🍕 Creampie",
                            emoji = if (isGP) "💋" else "💦 🍕",
                            subtitleChecked = if (isGP) "Com carinho" else "Creampie realizado",
                            checked = typeCreampie,
                            onCheckedChange = { checked ->
                                typeCreampie = checked
                                if (checked && countCreampie == 0) countCreampie = 1
                            },
                            count = countCreampie,
                            onCountChange = { countCreampie = it }
                        )

                        if (!isGP) {
                            FetishRow(
                                label = "🍑💦 Anal Creampie",
                                emoji = "🍑💦",
                                subtitleChecked = "Anal creampie realizado",
                                checked = typeAnalCreampie,
                                onCheckedChange = { checked ->
                                    typeAnalCreampie = checked
                                    if (checked && countAnalCreampie == 0) countAnalCreampie = 1
                                },
                                count = countAnalCreampie,
                                onCountChange = { countAnalCreampie = it }
                            )
                        }

                        FetishRow(
                            label = if (isGP) "👄 Oral s/camisinha" else "😛 Oral (ela -> mim)",
                            emoji = if (isGP) "👄" else "😛",
                            subtitleChecked = if (isGP) "Gulosa s/camisinha" else "Gulosa",
                            checked = typeOral,
                            onCheckedChange = { checked ->
                                typeOral = checked
                                if (checked && countOral == 0) countOral = 1
                            },
                            count = countOral,
                            onCountChange = { countOral = it }
                        )

                        FetishRow(
                            label = if (isGP) "🔄 69 c/camisinha" else "💦 Facial",
                            emoji = if (isGP) "🔄" else "💦",
                            subtitleChecked = if (isGP) "Invertido" else "Sem limites",
                            checked = typeFacial,
                            onCheckedChange = { checked ->
                                typeFacial = checked
                                if (checked && countFacial == 0) countFacial = 1
                            },
                            count = countFacial,
                            onCountChange = { countFacial = it }
                        )

                        FetishRow(
                            label = if (isGP) "💆‍♀️ Massagem erótica" else "🌊 Squirt",
                            emoji = if (isGP) "💆‍♀️" else "🌊",
                            subtitleChecked = if (isGP) "Massagem" else "Fonte Oculta",
                            checked = typeSquirt,
                            onCheckedChange = { checked ->
                                typeSquirt = checked
                                if (checked && countSquirt == 0) countSquirt = 1
                            },
                            count = countSquirt,
                            onCountChange = { countSquirt = it }
                        )

                        FetishRow(
                            label = if (isGP) "⛓️ Dominação / BDSM" else "👅 Garganta profunda",
                            emoji = if (isGP) "⛓️" else "👅",
                            subtitleChecked = if (isGP) "BDSM" else "Profunda",
                            checked = typeDeepthroat,
                            onCheckedChange = { checked ->
                                typeDeepthroat = checked
                                if (checked && countDeepthroat == 0) countDeepthroat = 1
                            },
                            count = countDeepthroat,
                            onCountChange = { countDeepthroat = it }
                        )

                        if (isGP) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Outras Práticas Extra GP:", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            CheckboxRow(label = "🍯 Beijo Grego", checked = gpBeijoGrego, onCheckedChange = { gpBeijoGrego = it })
                            CheckboxRow(label = "🔄 Inversão de Fetiche", checked = gpInversaoFetiche, onCheckedChange = { gpInversaoFetiche = it })
                            CheckboxRow(label = "👅 Garganta Profunda", checked = gpGargantaProfunda, onCheckedChange = { gpGargantaProfunda = it })
                            CheckboxRow(label = "👣 Fetiche de Pé", checked = gpFetichePe, onCheckedChange = { gpFetichePe = it })
                            CheckboxRow(label = "☝️ Fio Terra", checked = gpFioTerra, onCheckedChange = { gpFioTerra = it })
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Anotações/Comentários", color = textVariantColor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, focusedBorderColor = PrimaryGold, unfocusedTextColor = textColor, unfocusedBorderColor = textVariantColor.copy(alpha = 0.5f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = textVariantColor)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val parsedCost = costStr.toDoubleOrNull() ?: encontro.motelCost
                            val parsedRating = ratingStr.toDoubleOrNull() ?: encontro.rating
                            onConfirm(
                                encontro.copy(
                                    date = date,
                                    time = time,
                                    motelCost = parsedCost,
                                    typeOral = if (hadSex) typeOral else false,
                                    typeAnal = if (hadSex) typeAnal else false,
                                    typeVaginal = if (hadSex) typeVaginal else false,
                                    typeCreampie = if (hadSex) typeCreampie else false,
                                    typeAnalCreampie = if (hadSex) typeAnalCreampie else false,
                                    typeFacial = if (hadSex) typeFacial else false,
                                    typeSquirt = if (hadSex) typeSquirt else false,
                                    typeDeepthroat = if (hadSex) typeDeepthroat else false,
                                    countOral = if (hadSex && typeOral) countOral else 0,
                                    countAnal = if (hadSex && typeAnal) countAnal else 0,
                                    countVaginal = if (hadSex && typeVaginal) countVaginal else 0,
                                    countCreampie = if (hadSex && typeCreampie) countCreampie else 0,
                                    countAnalCreampie = if (hadSex && typeAnalCreampie) countAnalCreampie else 0,
                                    countFacial = if (hadSex && typeFacial) countFacial else 0,
                                    countSquirt = if (hadSex && typeSquirt) countSquirt else 0,
                                    countDeepthroat = if (hadSex && typeDeepthroat) countDeepthroat else 0,
                                    notes = notes.trim(),
                                    hadSex = hadSex,
                                    rating = parsedRating,
                                    
                                    // GP specific fields sync
                                    gpSexoVaginal = if (isGP && hadSex) typeVaginal else encontro.gpSexoVaginal,
                                    gpSexoAnal = if (isGP && hadSex) typeAnal else encontro.gpSexoAnal,
                                    gpOralSemCamisinha = if (isGP && hadSex) typeOral else encontro.gpOralSemCamisinha,
                                    gpBeijoNaBoca = if (isGP && hadSex) typeCreampie else encontro.gpBeijoNaBoca,
                                    gp69ComCamisinha = if (isGP && hadSex) typeFacial else encontro.gp69ComCamisinha,
                                    gpMassagemErotica = if (isGP && hadSex) typeSquirt else encontro.gpMassagemErotica,
                                    gpDominacaoBdsm = if (isGP && hadSex) typeDeepthroat else encontro.gpDominacaoBdsm,
                                    gpBeijoGrego = if (isGP && hadSex) gpBeijoGrego else encontro.gpBeijoGrego,
                                    gpInversaoFetiche = if (isGP && hadSex) gpInversaoFetiche else encontro.gpInversaoFetiche,
                                    gpGargantaProfunda = if (isGP && hadSex) gpGargantaProfunda else encontro.gpGargantaProfunda,
                                    gpFetichePe = if (isGP && hadSex) gpFetichePe else encontro.gpFetichePe,
                                    gpFioTerra = if (isGP && hadSex) gpFioTerra else encontro.gpFioTerra,
                                    gpGastoValor = if (isGP) parsedCost else encontro.gpGastoValor
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold)
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}

// ------------------- SCREEN 7: DATE SUGGESTIONS MODULE (LUGAR) -------------------

@Composable
fun DateSuggestionsScreen(viewModel: AgendaViewModel) {
    var selectedLocationFilter by remember { mutableStateOf("Minha localização") }
    var selectedCategory by remember { mutableStateOf("Motel") }
    var selectedSubCategory by remember { mutableStateOf<String?>(null) }

    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
    val cardColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White
    val context = LocalContext.current

    // Gather existing partners (women list)
    val partners by viewModel.allPartners.collectAsStateWithLifecycle()
    val womenList = remember(partners) { partners.filter { !it.isGP } }
    var selectedWoman by remember { mutableStateOf<Partner?>(null) }

    // Auto-select first woman if selection is null and list is not empty
    LaunchedEffect(womenList) {
        if (selectedWoman == null && womenList.isNotEmpty()) {
            selectedWoman = womenList.firstOrNull()
        }
    }

    // Coordinates or GPS location (syncs with ViewModel)
    var myCurrentGPSByWaze by remember(viewModel.userGpsAddress) { 
        mutableStateOf(if (viewModel.userGpsAddress.isNotBlank()) viewModel.userGpsAddress else "Campinas, SP") 
    }
    var showGPSModifierDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            requestAutomaticGPSLocationDetailed(context) { name, lat, lng ->
                myCurrentGPSByWaze = name
                viewModel.updateUserGps(lat, lng, name)
                viewModel.loadNearbySuggestions(
                    mode = selectedLocationFilter,
                    partnerId = selectedWoman?.id?.toString(),
                    category = selectedCategory,
                    subCategory = selectedSubCategory,
                    radiusInMeters = 7000
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            requestAutomaticGPSLocationDetailed(context) { name, lat, lng ->
                myCurrentGPSByWaze = name
                viewModel.updateUserGps(lat, lng, name)
                viewModel.loadNearbySuggestions(
                    mode = selectedLocationFilter,
                    partnerId = selectedWoman?.id?.toString(),
                    category = selectedCategory,
                    subCategory = selectedSubCategory,
                    radiusInMeters = 7000
                )
            }
        }
    }

    // Navigation Redirection Cache Sync
    LaunchedEffect(viewModel.destinationPartnerId, viewModel.destinationLocationFilter) {
        val destId = viewModel.destinationPartnerId
        if (destId != null) {
            val matchedWoman = womenList.find { it.id == destId }
            if (matchedWoman != null) {
                selectedWoman = matchedWoman
                selectedLocationFilter = viewModel.destinationLocationFilter
                selectedSubCategory = null
            }
            viewModel.destinationPartnerId = null // Reset
            viewModel.destinationLocationFilter = "Minha localização"
        }
    }

    // Dynamic fetch triggered on selection changes with strict 7km radius
    LaunchedEffect(selectedLocationFilter, selectedWoman, selectedCategory, selectedSubCategory, viewModel.userGpsLatitude, viewModel.userGpsLongitude, viewModel.userGpsAddress) {
        viewModel.loadNearbySuggestions(
            mode = selectedLocationFilter,
            partnerId = selectedWoman?.id?.toString(),
            category = selectedCategory,
            subCategory = selectedSubCategory,
            radiusInMeters = 7000
        )
    }

    val nearbySuggestions by viewModel.nearbySuggestions.collectAsStateWithLifecycle()
    val isLoadingSuggestions by viewModel.isLoadingSuggestions.collectAsStateWithLifecycle()

    // Dynamic resolution of city and reference address for calculations
    val (locationAddress, myGPSLocation) = remember(selectedLocationFilter, selectedWoman, myCurrentGPSByWaze, viewModel.userGpsAddress) {
        val gps = if (viewModel.userGpsAddress.isNotBlank()) viewModel.userGpsAddress else myCurrentGPSByWaze
        val addr = when (selectedLocationFilter) {
            "Minha localização" -> gps
            "Casa dela" -> {
                val wom = selectedWoman
                if (wom != null && wom.address.isNotBlank()) {
                    wom.address
                } else {
                    gps
                }
            }
            else -> gps
        }
        Pair(addr, gps)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title and Gemini Status Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📍 Sugestão de Date",
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // Gemini API Status Pill
            val apiBadgeColor = when (viewModel.apiKeyStatus) {
                "CONNECTED" -> Color(0xFF4CAF50)
                "TESTING" -> PrimaryGold
                else -> Color(0xFFEF5350)
            }
            val apiBadgeText = when (viewModel.apiKeyStatus) {
                "CONNECTED" -> "✨ Gemini: ✅ Ativa"
                "TESTING" -> "✨ Gemini: ⏳ Conectando..."
                else -> "✨ Gemini: ⚠️ Verificar"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(apiBadgeColor.copy(alpha = 0.12f))
                    .border(1.dp, apiBadgeColor.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                    .clickable { showGPSModifierDialog = false /* focus */ }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = apiBadgeText,
                    color = apiBadgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Dedicated Location Card (Minha Localização / Casa Dela) with Manual Override
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedLocationFilter == "Minha localização") "📍 MINHA LOCALIZAÇÃO ATUAL" else "🏠 ENDEREÇO DA MULHER",
                        color = PrimaryGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Raio máx: 7 km",
                        color = Color(0xFF4CAF50),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedLocationFilter == "Minha localização") {
                    val activeLoc = if (viewModel.userGpsAddress.isNotBlank()) viewModel.userGpsAddress else viewModel.userManualAddress
                    val isManual = viewModel.isManualLocationMode
                    Text(
                        text = activeLoc,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isManual) "✏️ Modo: Localização manual cadastrada pelo usuário" else "🛰️ Modo: GPS do celular",
                        color = textVariantColor,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                requestAutomaticGPSLocationDetailed(context) { name, lat, lng ->
                                    viewModel.updateUserGps(lat, lng, name)
                                    viewModel.loadNearbySuggestions(
                                        mode = selectedLocationFilter,
                                        partnerId = selectedWoman?.id?.toString(),
                                        category = selectedCategory,
                                        radiusInMeters = 7000
                                    )
                                    Toast.makeText(context, "GPS atualizado: $name", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold.copy(alpha = 0.15f), contentColor = PrimaryGold),
                            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("🛰️ Obter do GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showGPSModifierDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("✏️ Cadastrar Manualmente", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Casa dela
                    val wom = selectedWoman
                    if (wom != null) {
                        Text(
                            text = "${wom.name} • ${wom.address.ifBlank { "Sem endereço cadastrado" }}",
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (wom.address.isBlank()) {
                            Text(
                                text = "⚠️ A ficha desta mulher está sem endereço. O app usará sua localização como referência ou você pode editar a ficha dela.",
                                color = Color(0xFFEF5350),
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Text(
                            text = "Nenhuma mulher selecionada.",
                            color = textVariantColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        if (showGPSModifierDialog) {
            Dialog(onDismissRequest = { showGPSModifierDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(1.5.dp, PrimaryGold),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            "📍 Cadastrar Minha Localização Atual",
                            color = PrimaryGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Digite sua cidade, bairro ou rua atual (ex: 'Campinas, SP', 'Vila Madalena, São Paulo', 'Centro, Santos'). A API do Gemini buscará locais estritamente a menos de 7 km deste ponto.",
                            color = textVariantColor,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        var manualInputText by remember { mutableStateOf(viewModel.userManualAddress) }
                        OutlinedTextField(
                            value = manualInputText,
                            onValueChange = { manualInputText = it },
                            label = { Text("Minha Localização (Rua, Bairro, Cidade)", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = PrimaryGold
                            ),
                            singleLine = true
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showGPSModifierDialog = false }) {
                                Text("Cancelar", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (manualInputText.isNotBlank()) {
                                        viewModel.setManualLocation(manualInputText)
                                        viewModel.loadNearbySuggestions(
                                            mode = selectedLocationFilter,
                                            partnerId = selectedWoman?.id?.toString(),
                                            category = selectedCategory,
                                            radiusInMeters = 7000
                                        )
                                        Toast.makeText(context, "Localização salva: ${manualInputText.trim()}", Toast.LENGTH_SHORT).show()
                                    }
                                    showGPSModifierDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Salvar & Buscar Locais")
                            }
                        }
                    }
                }
            }
        }

        // Location Filters (Horizontal Selector)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Minha localização", "Casa dela").forEach { filter ->
                val isSelected = selectedLocationFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (isSelected) PrimaryGold.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) PrimaryGold else (if (viewModel.isDarkTheme) Color.LightGray.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.3f)),
                            RoundedCornerShape(100.dp)
                        )
                        .clickable {
                            selectedLocationFilter = filter
                            selectedSubCategory = null // reset selection
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) PrimaryGold else textVariantColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // If Casa dela selected, show Woman Selection Tray
        if (selectedLocationFilter == "Casa dela") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "👩 Selecione a Ficha da Mulher:",
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (womenList.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nenhuma mulher cadastrada na aba Fichas!",
                                color = Color(0xFFEF5350),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Quick horizontal list of women to single select
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            womenList.forEach { wom ->
                                val isSelectedWom = selectedWoman?.id == wom.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelectedWom) PrimaryGold.copy(alpha = 0.15f) else cardColor.copy(alpha = 0.5f))
                                        .border(
                                            1.dp,
                                            if (isSelectedWom) PrimaryGold else Color.Gray.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedWoman = wom
                                            selectedSubCategory = null
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = wom.name,
                                        color = if (isSelectedWom) PrimaryGold else textColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Display her address or warning if address is empty
                        selectedWoman?.let { wom ->
                            if (wom.address.isEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x15EF5350))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Aviso: ${wom.name} não tem endereço cadastrado. Vá em Fichas -> Editar Ficha para salvar o endereço dela!",
                                        color = Color(0xFFE57373),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Home, contentDescription = "Home", tint = PrimaryGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Endereço dela cadastrado: ${wom.address}",
                                        color = textVariantColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Categorias solicitadas: Motel, Parques, Bar/Lanches, Restaurante, Shoppings
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryGridCard(
                label = "Motel",
                icon = "🏩",
                isSelected = selectedCategory == "Motel",
                onClick = { selectedCategory = "Motel"; selectedSubCategory = null },
                modifier = Modifier.weight(1f),
                viewModel = viewModel
            )
            CategoryGridCard(
                label = "Parques",
                icon = "🌳",
                isSelected = selectedCategory == "Parques",
                onClick = { selectedCategory = "Parques"; selectedSubCategory = null },
                modifier = Modifier.weight(1f),
                viewModel = viewModel
            )
            CategoryGridCard(
                label = "Shoppings",
                icon = "🛍️",
                isSelected = selectedCategory == "Shoppings",
                onClick = { selectedCategory = "Shoppings"; selectedSubCategory = null },
                modifier = Modifier.weight(1f),
                viewModel = viewModel
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryGridCard(
                label = "Bar/Lanches",
                icon = "🍔",
                isSelected = selectedCategory == "Bar/Lanches",
                onClick = { selectedCategory = "Bar/Lanches"; selectedSubCategory = null },
                modifier = Modifier.weight(1f),
                viewModel = viewModel
            )
            CategoryGridCard(
                label = "Restaurante",
                icon = "🍽️",
                isSelected = selectedCategory == "Restaurante",
                onClick = { selectedCategory = "Restaurante"; selectedSubCategory = null },
                modifier = Modifier.weight(1f),
                viewModel = viewModel
            )
        }

        // Subcategorias solicitadas para cada categoria
        val subCategories = when (selectedCategory) {
            "Motel" -> listOf(
                Pair("💵 Barato / Econômico", "💵"),
                Pair("💎 Ostentação / Luxo", "💎"),
                Pair("🛁 Com Hidro / Ofurô", "🛁"),
                Pair("🌹 Suíte Temática", "🌹")
            )
            "Parques" -> listOf(
                Pair("🌳 Bosque & Natureza", "🌳"),
                Pair("🏃 Pista & Orla", "🏃"),
                Pair("🌊 Lago & Represa", "🌊"),
                Pair("🌺 Jardim Botânico", "🌺")
            )
            "Bar/Lanches" -> listOf(
                Pair("🍔 Hamburgueria", "🍔"),
                Pair("🍺 Bar & Boteco", "🍺"),
                Pair("🍸 Drinks & Coquetelaria", "🍸"),
                Pair("🍣 Sushi & Petiscos", "🍣"),
                Pair("☕ Cafeteria & Doceria", "☕")
            )
            "Restaurante" -> listOf(
                Pair("🕯️ Romântico a Dois", "🕯️"),
                Pair("🍝 Italiano & Massas", "🍝"),
                Pair("🍱 Japonês Premium", "🍱"),
                Pair("🥩 Churrascaria & Carnes", "🥩"),
                Pair("🥂 Alta Gastronomia", "🥂")
            )
            else -> listOf( // "Shoppings"
                Pair("🛍️ Shopping Center", "🛍️"),
                Pair("🎬 Cinema", "🎬"),
                Pair("🍨 Sorveteria & Gelato", "🍨"),
                Pair("🎳 Boliche & Jogos", "🎳")
            )
        }

        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedSubCategory == null,
                    onClick = { selectedSubCategory = null },
                    label = { Text("✨ Todos", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGold,
                        selectedLabelColor = OnPrimaryGold,
                        containerColor = cardColor,
                        labelColor = textColor
                    )
                )
            }
            items(subCategories.size) { idx ->
                val (subName, _) = subCategories[idx]
                FilterChip(
                    selected = selectedSubCategory == subName,
                    onClick = { selectedSubCategory = if (selectedSubCategory == subName) null else subName },
                    label = { Text(subName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGold,
                        selectedLabelColor = OnPrimaryGold,
                        containerColor = cardColor,
                        labelColor = textColor
                    )
                )
            }
        }

        // Location descriptor banner
        val descriptionLabel = when (selectedLocationFilter) {
            "Minha localização" -> "📍 Próximo à sua localização atual ($locationAddress)"
            "Casa dela" -> "🏠 Próximo à casa dela (${selectedWoman?.name ?: "Nenhuma selecionada"})"
            else -> "↔️ No meio do caminho"
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = descriptionLabel,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Raio máx 7 km", color = Color(0xFF4CAF50), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔎 LOCAIS REAIS ENCONTRADOS (${nearbySuggestions.size})",
                color = PrimaryGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Base Cartográfica Oficial",
                color = textVariantColor,
                fontSize = 9.sp
            )
        }

        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

        if (isLoadingSuggestions) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Buscando estabelecimentos reais com precisão cartográfica...", color = textColor, fontSize = 12.sp)
                }
            }
        } else if (nearbySuggestions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📍", fontSize = 28.sp)
                    Text(
                        "Nenhum estabelecimento encontrado nesta categoria a até 7 km de '$locationAddress'.",
                        color = textColor,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Tente selecionar '✨ Todos' ou trocar a categoria acima para ver locais próximos.",
                        color = textVariantColor,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                nearbySuggestions.forEach { place ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = place.name,
                                        color = textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = place.category,
                                            color = PrimaryGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "• Faixa: ${place.priceBracket}",
                                            color = textVariantColor,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    val distLabel = when (selectedLocationFilter) {
                                        "Minha localização" -> "A %.1f km de você".format(java.util.Locale.US, place.distanceInKm)
                                        "Casa dela" -> "A %.1f km da casa de %s".format(java.util.Locale.US, place.distanceInKm, selectedWoman?.name ?: "dela")
                                        else -> "A %.1f km do meio do caminho".format(java.util.Locale.US, place.distanceInKm)
                                    }
                                    Text(
                                        text = distLabel,
                                        color = Color(0xFF4CAF50),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PrimaryGold.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "${place.rating} ⭐", color = PrimaryGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // Real address banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏢 ${place.address}",
                                    color = textVariantColor,
                                    fontSize = 10.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Copiar",
                                    color = PrimaryGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(place.address))
                                        android.widget.Toast.makeText(context, "Endereço copiado!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val query = "${place.name}, ${place.address}"
                                        val encodedQuery = Uri.encode(query)
                                        val intentUri = Uri.parse("geo:${place.latitude},${place.longitude}?q=$encodedQuery")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, intentUri).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            val fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Abrir no Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        val query = "${place.name}, ${place.address}"
                                        val encodedQuery = Uri.encode(query)
                                        val wazeUri = Uri.parse("waze://?ll=${place.latitude},${place.longitude}&navigate=yes")
                                        val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri)
                                        try {
                                            context.startActivity(wazeIntent)
                                        } catch (e: Exception) {
                                            val fallbackWazeUri = Uri.parse("https://waze.com/ul?q=$encodedQuery&navigate=yes")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackWazeUri))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C1FF), contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Abrir no Waze", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun CategoryGridCard(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgendaViewModel
) {
    val cardColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White
    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, PrimaryGold)
    } else {
        BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.15f))
    }
    Card(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryGold.copy(alpha = 0.12f) else cardColor
        ),
        border = borderStroke,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (isSelected) PrimaryGold else (if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class Venue(val name: String, val rating: String, val price: String, val address: String, val distance: String = "1.5 km")

fun getRawVenueList(category: String, subCategory: String, locationFilter: String, woman: Partner?, locationAddress: String = ""): List<Venue> {
    val addrNormalized = locationAddress.lowercase()
    val city = when {
        addrNormalized.contains("campinas") -> "Campinas"
        addrNormalized.contains("santos") -> "Santos"
        addrNormalized.contains("rio") || addrNormalized.contains("rj") -> "Rio de Janeiro"
        else -> {
            if (locationFilter == "Casa dela" && woman != null && woman.address.isNotEmpty()) {
                val addr = woman.address.lowercase()
                if (addr.contains("campinas")) "Campinas"
                else if (addr.contains("santos")) "Santos"
                else if (addr.contains("rio") || addr.contains("rj")) "Rio de Janeiro"
                else "São Paulo"
            } else "São Paulo"
        }
    }

    val cat = category.lowercase().trim()
    val sub = subCategory.lowercase().trim()

    return when {
        cat.contains("motel") -> {
            when {
                sub.contains("barat") || sub.contains("econ") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Motel Champion", "4.3 ⭐ (950)", "R$ 90 - R$ 180", "R. Jacy Teixeira Camargo, 50 - Jardim do Lago, Campinas"),
                            Venue("Motel Drops Campinas", "4.4 ⭐ (820)", "R$ 110 - R$ 220", "Rodovia Dom Pedro I, km 131, Campinas"),
                            Venue("Motel Saville", "4.2 ⭐ (700)", "R$ 80 - R$ 160", "Av. John Boyd Dunlop, 1200 - Jardim Ipaussurama, Campinas")
                        )
                    } else if (city == "Santos") {
                        listOf(
                            Venue("Motel Sagitário", "4.3 ⭐ (850)", "R$ 100 - R$ 210", "Rodovia Anchieta, km 64, Santos"),
                            Venue("Motel Skorpios", "4.1 ⭐ (620)", "R$ 90 - R$ 180", "Av. Nossa Senhora de Fátima, Santos")
                        )
                    } else {
                        listOf(
                            Venue("Motel Tout", "4.3 ⭐ (2.1k)", "R$ 110 - R$ 230", "Av. dos Bandeirantes, 3990 - Campo Belo, São Paulo"),
                            Venue("Motel Confidence", "4.2 ⭐ (1.5k)", "R$ 95 - R$ 190", "Av. Prof. Francisco Morato, 2500 - Caxingui, São Paulo")
                        )
                    }
                }
                sub.contains("ostenta") || sub.contains("luxo") || sub.contains("premium") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Motel My Flowers Luxury", "4.5 ⭐ (1.3k)", "R$ 190 - R$ 450", "Rodovia Dom Pedro I, km 135, Campinas"),
                            Venue("Prime Motel Campinas", "4.6 ⭐ (980)", "R$ 210 - R$ 490", "Rodovia Governador Adhemar Pereira de Barros, km 118, Campinas")
                        )
                    } else if (city == "Santos") {
                        listOf(
                            Venue("Motel Fantasy Santos", "4.5 ⭐ (910)", "R$ 180 - R$ 420", "Rodovia dos Imigrantes, Santos")
                        )
                    } else {
                        listOf(
                            Venue("Motel Lush Ipiranga", "4.7 ⭐ (5.5k)", "R$ 220 - R$ 650", "Av. do Estado, 6116 - Ipiranga, São Paulo"),
                            Venue("Motel Swing Itaim", "4.5 ⭐ (4.1k)", "R$ 200 - R$ 580", "Av. Pres. Juscelino Kubitschek, 300 - Itaim Bibi, São Paulo"),
                            Venue("Motel Apple Barra Funda", "4.6 ⭐ (3.2k)", "R$ 190 - R$ 520", "R. Quirino dos Santos, 191 - Barra Funda, São Paulo")
                        )
                    }
                }
                sub.contains("hidro") || sub.contains("ofurô") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Motel My Flowers (Suíte Hidro Master)", "4.6 ⭐ (900)", "R$ 240 - R$ 480", "Rodovia Dom Pedro I, km 135, Campinas"),
                            Venue("Motel Drops (Suíte Hidro SPA)", "4.5 ⭐ (750)", "R$ 200 - R$ 420", "Rodovia Dom Pedro I, km 131, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("Motel Lush Spa & Ofurô", "4.8 ⭐ (4.8k)", "R$ 280 - R$ 680", "Av. do Estado, 6116 - Ipiranga, São Paulo"),
                            Venue("Motel Acaso Premium Romana", "4.6 ⭐ (2.8k)", "R$ 250 - R$ 540", "Av. Salim Farah Maluf, 6000 - Mooca, São Paulo")
                        )
                    }
                }
                else -> { // Temática
                    if (city == "Campinas") {
                        listOf(
                            Venue("Motel Flowers 50 Tons", "4.5 ⭐ (1.1k)", "R$ 240 - R$ 500", "Rodovia Dom Pedro I, km 135, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("Motel Lush (Suíte Cine Privé)", "4.7 ⭐ (3k)", "R$ 280 - R$ 620", "Av. do Estado, 6116 - Ipiranga, São Paulo"),
                            Venue("Motel Play Medieval & Pole", "4.4 ⭐ (1.4k)", "R$ 190 - R$ 410", "Marginal Pinheiros, São Paulo")
                        )
                    }
                }
            }
        }
        cat.contains("parque") -> {
            when {
                sub.contains("bosque") || sub.contains("natureza") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Bosque dos Jequitibás", "4.5 ⭐ (7.1k)", "Grátis", "R. Cel. Quirino, 2 - Bosque, Campinas"),
                            Venue("Mata de Santa Genebra", "4.6 ⭐ (1.2k)", "Grátis", "R. Mata Atlântica, 447 - Bosque de Barão, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("Parque da Cantareira (Horto Florestal)", "4.7 ⭐ (14k)", "Grátis", "R. do Horto, 931 - Horto Florestal, São Paulo"),
                            Venue("Parque Burle Marx", "4.7 ⭐ (16k)", "Grátis", "Av. Dona Helena Pereira de Moraes, 200 - Vila Andrade, São Paulo")
                        )
                    }
                }
                sub.contains("pista") || sub.contains("orla") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Pista da Lagoa do Taquaral", "4.8 ⭐ (15k)", "Grátis", "Av. Dr. Heitor Penteado, s/n - Taquaral, Campinas")
                        )
                    } else if (city == "Santos") {
                        listOf(
                            Venue("Jardins da Orla de Santos", "4.8 ⭐ (18k)", "Grátis", "Av. Vicente de Carvalho - Gonzaga, Santos"),
                            Venue("Emissário Submarino Santos", "4.6 ⭐ (5.5k)", "Grátis", "Av. Pres. Wilson - José Menino, Santos")
                        )
                    } else {
                        listOf(
                            Venue("Parque Ibirapuera", "4.8 ⭐ (130k)", "Grátis", "Av. Pedro Álvares Cabral - Vila Mariana, São Paulo"),
                            Venue("Parque Villa-Lobos", "4.7 ⭐ (45k)", "Grátis", "Av. Prof. Fonseca Rodrigues, 2001 - Alto de Pinheiros, São Paulo")
                        )
                    }
                }
                sub.contains("lago") || sub.contains("represa") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Parque Portugal (Lagoa do Taquaral)", "4.8 ⭐ (28k)", "Grátis", "Av. Dr. Heitor Penteado, 1671 - Parque Taquaral, Campinas"),
                            Venue("Lago do Café", "4.4 ⭐ (3.2k)", "Grátis", "Av. Dr. Heitor Penteado, 2145 - Parque Taquaral, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("Parque do Carmo (Lago)", "4.6 ⭐ (22k)", "Grátis", "Av. Afonso de Sampaio e Sousa, 951 - Itaquera, São Paulo"),
                            Venue("Represa de Guarapiranga (Parque da Barragem)", "4.4 ⭐ (6.1k)", "Grátis", "Av. Atlântica, São Paulo")
                        )
                    }
                }
                else -> { // Jardim Botânico
                    if (city == "Campinas") {
                        listOf(
                            Venue("Jardim Botânico do IAC", "4.5 ⭐ (1.5k)", "Grátis", "Av. Barão de Itapura, 1481 - Botafogo, Campinas")
                        )
                    } else if (city == "Santos") {
                        listOf(
                            Venue("Jardim Botânico de Santos Chico Mendes", "4.6 ⭐ (3.8k)", "Grátis", "R. João Fraccaroli, s/n - Bom Retiro, Santos")
                        )
                    } else {
                        listOf(
                            Venue("Jardim Botânico de São Paulo", "4.7 ⭐ (21k)", "R$ 15 - R$ 25", "Av. Miguel Estefno, 3031 - Vila Água Funda, São Paulo")
                        )
                    }
                }
            }
        }
        cat.contains("bar") || cat.contains("lanche") || cat.contains("barato") -> {
            when {
                sub.contains("hamburg") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Cabana Burger Cambuí", "4.6 ⭐ (3.5k)", "R$ 35 - R$ 65", "R. Dr. Guilherme da Silva, 400 - Cambuí, Campinas"),
                            Venue("Bullguer Campinas", "4.4 ⭐ (2.1k)", "R$ 30 - R$ 55", "R. Coronel Silva Teles, 532 - Cambuí, Campinas"),
                            Venue("Patties Campinas", "4.5 ⭐ (1.4k)", "R$ 25 - R$ 45", "R. Maria Monteiro, 800 - Cambuí, Campinas")
                        )
                    } else if (city == "Santos") {
                        listOf(
                            Venue("Seven Burgers", "4.6 ⭐ (1.6k)", "R$ 35 - R$ 60", "R. Dr. Marcílio Dias, 46 - Gonzaga, Santos"),
                            Venue("Madero Container Santos", "4.3 ⭐ (2.4k)", "R$ 45 - R$ 75", "Av. Vicente de Carvalho, Santos")
                        )
                    } else {
                        listOf(
                            Venue("Patties Burger Jardins", "4.5 ⭐ (7.1k)", "R$ 25 - R$ 45", "Rua Professor Arthur Ramos, 795 - Pinheiros, São Paulo"),
                            Venue("Cabana Burger Oscar Freire", "4.6 ⭐ (9.2k)", "R$ 35 - R$ 65", "Rua Oscar Freire, 560 - Cerqueira César, São Paulo"),
                            Venue("Z-Deli Sandwich Shop", "4.7 ⭐ (8.5k)", "R$ 40 - R$ 75", "Rua Haddock Lobo, 1386 - Cerqueira César, São Paulo")
                        )
                    }
                }
                sub.contains("bar") || sub.contains("boteco") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Seo Rosa Cambuí", "4.6 ⭐ (4.8k)", "R$ 40 - R$ 85", "R. Dr. Emílio Ribas, 567 - Cambuí, Campinas"),
                            Venue("Bar do Alemão Campinas", "4.5 ⭐ (3.2k)", "R$ 50 - R$ 95", "Av. Barão de Itapura, 2088 - Botafogo, Campinas"),
                            Venue("Grainne's Pub Cambuí", "4.5 ⭐ (3.9k)", "R$ 45 - R$ 90", "R. Glicério, 1665 - Cambuí, Campinas")
                        )
                    } else if (city == "Santos") {
                        listOf(
                            Venue("Bar do Toninho", "4.6 ⭐ (3.1k)", "R$ 35 - R$ 70", "Av. Dr. Epitácio Pessoa, 241 - Embaré, Santos"),
                            Venue("Australiano Bar Santos", "4.4 ⭐ (1.8k)", "R$ 40 - R$ 80", "Av. Marechal Floriano Peixoto - Gonzaga, Santos")
                        )
                    } else {
                        listOf(
                            Venue("Bar Veloso (Vila Mariana)", "4.7 ⭐ (16k)", "R$ 35 - R$ 75", "R. Conceição Veloso, 54 - Vila Mariana, São Paulo"),
                            Venue("Pirajá Faria Lima", "4.5 ⭐ (7.8k)", "R$ 45 - R$ 90", "Av. Brig. Faria Lima, 64 - Pinheiros, São Paulo"),
                            Venue("Bar Brahma Centro", "4.5 ⭐ (18k)", "R$ 50 - R$ 100", "Av. São João, 677 - Centro, São Paulo")
                        )
                    }
                }
                sub.contains("drink") || sub.contains("coquetel") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Rooftop Cambuí Lounge", "4.5 ⭐ (1.2k)", "R$ 45 - R$ 95", "Av. Júlio de Mesquita, Cambuí, Campinas"),
                            Venue("The Lord's Pub & Drinks", "4.4 ⭐ (890)", "R$ 40 - R$ 85", "R. Américo Brasiliense, Cambuí, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("SubAstor Vila Madalena", "4.7 ⭐ (4.5k)", "R$ 55 - R$ 110", "R. Delfina, 163 - Vila Madalena, São Paulo"),
                            Venue("Guarita Bar Pinheiros", "4.6 ⭐ (3.8k)", "R$ 45 - R$ 90", "R. Simão Álvares, 952 - Pinheiros, São Paulo"),
                            Venue("Frank Bar Maksoud", "4.6 ⭐ (2.2k)", "R$ 60 - R$ 120", "Alameda Campinas, 150 - Bela Vista, São Paulo")
                        )
                    }
                }
                sub.contains("sushi") || sub.contains("petisco") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Kaishi Sushi Cambuí", "4.5 ⭐ (2.1k)", "R$ 65 - R$ 120", "R. Cel. Silva Teles, 340 - Cambuí, Campinas"),
                            Venue("Taisho Sushi Campinas", "4.4 ⭐ (1.7k)", "R$ 60 - R$ 110", "R. Maria Monteiro, Cambuí, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("Mori Chazeria Jardins", "4.6 ⭐ (3.2k)", "R$ 70 - R$ 140", "R. da Consolação, 3610 - Cerqueira César, São Paulo"),
                            Venue("Izakaya Kurotaki", "4.6 ⭐ (1.8k)", "R$ 55 - R$ 110", "R. da Glória, Liberdade, São Paulo")
                        )
                    }
                }
                else -> { // Cafeteria & Doceria
                    if (city == "Campinas") {
                        listOf(
                            Venue("Café Cambuí", "4.6 ⭐ (1.3k)", "R$ 20 - R$ 45", "R. Maria Monteiro, 1200 - Cambuí, Campinas"),
                            Venue("Fran's Café Cambuí", "4.2 ⭐ (950)", "R$ 18 - R$ 38", "R. Coronel Silva Teles, Cambuí, Campinas"),
                            Venue("Maria Antonieta Boulangerie", "4.7 ⭐ (4.1k)", "R$ 30 - R$ 65", "R. Cel. Quirino, 1239 - Cambuí, Campinas")
                        )
                    } else if (city == "Santos") {
                        listOf(
                            Venue("Café Carioca", "4.5 ⭐ (1.1k)", "R$ 15 - R$ 35", "Praça Visconde de Mauá, 1 - Centro, Santos"),
                            Venue("Revo Coffee Santos", "4.7 ⭐ (1.8k)", "R$ 25 - R$ 55", "Av. Epitácio Pessoa, 737 - Ponta da Praia, Santos")
                        )
                    } else {
                        listOf(
                            Venue("Coffee Lab Pinheiros", "4.7 ⭐ (9.5k)", "R$ 20 - R$ 50", "R. Fradique Coutinho, 1340 - Pinheiros, São Paulo"),
                            Venue("Padaria Bella Paulista", "4.5 ⭐ (18k)", "R$ 25 - R$ 65", "Rua Haddock Lobo, 354 - Cerqueira César, São Paulo")
                        )
                    }
                }
            }
        }
        cat.contains("restaurante") || cat.contains("ostenta") -> {
            when {
                sub.contains("românt") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Cantina Bellini Ristorante", "4.7 ⭐ (3.1k)", "R$ 90 - R$ 180", "Av. José de Souza Campos, 425 - Cambuí, Campinas"),
                            Venue("L'Alouette Campinas", "4.6 ⭐ (1.5k)", "R$ 80 - R$ 160", "R. Padre Almeida, 645 - Cambuí, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("Terraço Itália", "4.6 ⭐ (12k)", "R$ 180 - R$ 380", "Av. Ipiranga, 344 - 41º andar - Centro Histórico, São Paulo"),
                            Venue("Ruella Bistrô Vila Olímpia", "4.6 ⭐ (3.2k)", "R$ 110 - R$ 220", "R. João Cachoeira, 1507 - Vila Olímpia, São Paulo")
                        )
                    }
                }
                sub.contains("italian") || sub.contains("massa") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Cantina do Bacco", "4.6 ⭐ (2.4k)", "R$ 75 - R$ 150", "R. Maria Monteiro, 1296 - Cambuí, Campinas"),
                            Venue("Famiglia Giuliano Cambuí", "4.5 ⭐ (1.9k)", "R$ 70 - R$ 140", "R. Dr. Emílio Ribas, Cambuí, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("Cantina Famiglia Mancini", "4.6 ⭐ (24k)", "R$ 110 - R$ 230", "R. Avanhandava, 81 - Bela Vista, São Paulo"),
                            Venue("Nino Ristorante Itaim", "4.5 ⭐ (5.1k)", "R$ 120 - R$ 260", "R. Jerônimo da Veiga, 30 - Itaim Bibi, São Paulo")
                        )
                    }
                }
                sub.contains("japon") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Kaishi Premium Cambuí", "4.6 ⭐ (2.8k)", "R$ 120 - R$ 240", "R. Cel. Silva Teles, 340 - Cambuí, Campinas"),
                            Venue("Kindai Campinas", "4.5 ⭐ (3.1k)", "R$ 110 - R$ 220", "Av. José de Souza Campos, 425 - Cambuí, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("Nakka Itaim Bibi", "4.7 ⭐ (4.2k)", "R$ 190 - R$ 380", "R. Pedroso Alvarenga, 1110 - Itaim Bibi, São Paulo"),
                            Venue("Kosushi Shopping Cidade Jardim", "4.6 ⭐ (1.8k)", "R$ 220 - R$ 450", "Av. Magalhães de Castro, 12000 - São Paulo")
                        )
                    }
                }
                sub.contains("churrasc") || sub.contains("carne") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Churrascaria Pobre Juan Campinas", "4.7 ⭐ (3.8k)", "R$ 140 - R$ 280", "Shopping Iguatemi Campinas, Campinas"),
                            Venue("Sulina Churrascaria Barão", "4.5 ⭐ (4.1k)", "R$ 85 - R$ 160", "Av. Albino J. B. de Oliveira, 2000 - Barão Geraldo, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("Fogo de Chão Jardins", "4.7 ⭐ (8.9k)", "R$ 180 - R$ 320", "R. Augusta, 2077 - Cerqueira César, São Paulo"),
                            Venue("Barbacoa Itaim", "4.6 ⭐ (5.6k)", "R$ 170 - R$ 310", "R. Dr. Renato Paes de Barros, 65 - Itaim Bibi, São Paulo")
                        )
                    }
                }
                else -> { // Alta Gastronomia
                    if (city == "Campinas") {
                        listOf(
                            Venue("Bellini Ristorante Cambuí", "4.8 ⭐ (3.5k)", "R$ 150 - R$ 300", "Av. José de Souza Campos, 425 - Cambuí, Campinas"),
                            Venue("Maialini Cambuí", "4.7 ⭐ (1.8k)", "R$ 130 - R$ 260", "R. Emílio Ribas, 1247 - Cambuí, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("D.O.M. Restaurante (Alex Atala)", "4.8 ⭐ (5.8k)", "R$ 350 - R$ 750", "R. Barão de Capanema, 549 - Cerqueira César, São Paulo"),
                            Venue("Restaurante Fasano Jardins", "4.8 ⭐ (3.2k)", "R$ 300 - R$ 680", "R. Vitório Fasano, 88 - Cerqueira César, São Paulo")
                        )
                    }
                }
            }
        }
        else -> { // Shoppings
            when {
                sub.contains("cinema") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Cinépolis VIP Shopping P. Dom Pedro", "4.6 ⭐ (4.2k)", "R$ 45 - R$ 90", "Av. Guilherme Campos, 500 - Jardim Santa Genebra, Campinas"),
                            Venue("Kinoplex Iguatemi Campinas", "4.5 ⭐ (3.1k)", "R$ 35 - R$ 70", "Av. Iguatemi, 777 - Vila Brandina, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("Cinemark VIP Cidade São Paulo", "4.7 ⭐ (4.8k)", "R$ 50 - R$ 95", "Av. Paulista, 1230 - Bela Vista, São Paulo"),
                            Venue("Cinépolis VIP JK Iguatemi", "4.8 ⭐ (4.2k)", "R$ 65 - R$ 120", "Av. Pres. Juscelino Kubitschek, 2041 - Itaim Bibi, São Paulo")
                        )
                    }
                }
                sub.contains("sorvete") || sub.contains("gelato") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Bacio di Latte Cambuí", "4.7 ⭐ (3.9k)", "R$ 18 - R$ 38", "R. Dr. Emílio Ribas, 1000 - Cambuí, Campinas"),
                            Venue("Gelato Borelli Cambuí", "4.6 ⭐ (2.2k)", "R$ 16 - R$ 34", "R. Coronel Silva Teles, Cambuí, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("Bacio di Latte Oscar Freire", "4.8 ⭐ (9.2k)", "R$ 18 - R$ 38", "Rua Oscar Freire, 136 - Cerqueira César, São Paulo"),
                            Venue("Cuor di Crema Vila Nova Conceição", "4.6 ⭐ (1.8k)", "R$ 17 - R$ 35", "R. João Lourenço, 388 - São Paulo")
                        )
                    }
                }
                sub.contains("boliche") || sub.contains("jogo") || sub.contains("arcade") -> {
                    if (city == "Campinas") {
                        listOf(
                            Venue("Boliche Dom Pedro", "4.4 ⭐ (1.8k)", "R$ 80 - R$ 160/hora", "Parque D. Pedro Shopping, Campinas")
                        )
                    } else {
                        listOf(
                            Venue("Villa Bowling West Plaza", "4.5 ⭐ (2.8k)", "R$ 90 - R$ 180/hora", "Av. Francisco Matarazzo - Água Branca, São Paulo"),
                            Venue("Villa Bowling Vila Olímpia", "4.5 ⭐ (3.6k)", "R$ 100 - R$ 220/hora", "R. Olimpíadas, 360 - Vila Olímpia, São Paulo")
                        )
                    }
                }
                else -> { // Shopping Center
                    if (city == "Campinas") {
                        listOf(
                            Venue("Parque D. Pedro Shopping", "4.7 ⭐ (35k)", "Completo", "Av. Guilherme Campos, 500 - Jardim Santa Genebra, Campinas"),
                            Venue("Shopping Iguatemi Campinas", "4.7 ⭐ (24k)", "Sofisticado", "Av. Iguatemi, 777 - Vila Brandina, Campinas"),
                            Venue("Galleria Shopping Campinas", "4.6 ⭐ (12k)", "Open Mall", "Rod. D. Pedro I, km 131,5 - Jardim Nilópolis, Campinas")
                        )
                    } else if (city == "Santos") {
                        listOf(
                            Venue("Praiamar Shopping Santos", "4.6 ⭐ (16k)", "Completo", "R. Alexandre Martins, 80 - Aparecida, Santos"),
                            Venue("Miramar Shopping Santos", "4.4 ⭐ (8.2k)", "Tradicional", "R. Euclides da Cunha, 21 - Gonzaga, Santos")
                        )
                    } else {
                        listOf(
                            Venue("Morumbi Shopping", "4.7 ⭐ (32k)", "Completo", "Av. Roque Petroni Júnior, 1089 - Santo Amaro, São Paulo"),
                            Venue("Shopping JK Iguatemi", "4.7 ⭐ (15k)", "Marcas de Luxo", "Av. Pres. Juscelino Kubitschek, 2041 - Itaim Bibi, São Paulo"),
                            Venue("Shopping Pátio Higienópolis", "4.6 ⭐ (14k)", "Gourmet", "R. Dr. Veiga Filho, 133 - Higienópolis, São Paulo")
                        )
                    }
                }
            }
        }
    }
}

fun calculateRelationshipDuration(firstDateStr: String): String {
    if (firstDateStr.isBlank()) return ""
    try {
        val sdfYmd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDmy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val parsedDate: Date = try {
            val d = if (firstDateStr.contains("-")) sdfYmd.parse(firstDateStr) else sdfDmy.parse(firstDateStr)
            d ?: return ""
        } catch (e: Exception) {
            return ""
        }

        val startCal = Calendar.getInstance().apply { time = parsedDate }
        val startYear = startCal.get(Calendar.YEAR)
        val startMonth = startCal.get(Calendar.MONTH) + 1 // 1-indexed
        val startDay = startCal.get(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()
        val currentYear = today.get(Calendar.YEAR)
        val currentMonth = today.get(Calendar.MONTH) + 1 // 1-indexed
        val currentDay = today.get(Calendar.DAY_OF_MONTH)

        var years = currentYear - startYear
        var months = currentMonth - startMonth
        var days = currentDay - startDay

        if (days < 0) {
            months -= 1
            val prevMonthCal = Calendar.getInstance()
            prevMonthCal.set(Calendar.YEAR, currentYear)
            prevMonthCal.set(Calendar.MONTH, currentMonth - 2) // previous month 0-indexed
            val daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            days += daysInPrevMonth
        }

        if (months < 0) {
            years -= 1
            months += 12
        }

        if (years < 0) {
            return "0a/0m/0d"
        }

        return "${years}a/${months}m/${days}d"
    } catch (e: Exception) {
        return ""
    }
}

fun getVenueList(category: String, subCategory: String, locationFilter: String, woman: Partner?, locationAddress: String = ""): List<Venue> {
    val rawList = getRawVenueList(category, subCategory, locationFilter, woman, locationAddress)
    return rawList.map { venue ->
        val rawHash = venue.name.hashCode() + venue.address.hashCode()
        val hash = (if (rawHash == Int.MIN_VALUE) 0 else Math.abs(rawHash)) % 50
        val distVal = 0.8 + (hash / 10.0) // 0.8 to 5.7 km (strictly within 7 km)
        
        val distLabel = if (locationFilter == "Casa dela" && woman != null) {
            "%.1f km da casa de %s".format(java.util.Locale.US, distVal, woman.name)
        } else {
            "%.1f km de você".format(java.util.Locale.US, distVal)
        }
        
        // Mantém sempre o endereço REAL e OFICIAL do estabelecimento para Waze e Google Maps
        venue.copy(distance = distLabel, address = venue.address)
    }
}

// ------------------- SECURITY LOGIN GATE PANEL (LOCK) -------------------

@Composable
fun AgendaLockScreen(viewModel: AgendaViewModel) {
    val context = LocalContext.current
    val activity = remember(context) {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is FragmentActivity) {
                return@remember currentContext
            }
            currentContext = currentContext.baseContext
        }
        null
    }
    var lockScreenMode by remember { mutableStateOf("select") } // "select", "pin"
    var codeTyped by remember { mutableStateOf("") }
    val correctCode = viewModel.pinCode

    val bgApp = DeepDarkBackground
    val keyColor = Level1DarkSurface

    fun launchScreenBiometric() {
        if (activity != null) {
            try {
                val executor = ContextCompat.getMainExecutor(activity)
                val biometricPrompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(context.applicationContext, "Erro Biometria: $errString. Use o PIN por senha.", Toast.LENGTH_LONG).show()
                            lockScreenMode = "pin"
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            viewModel.isAppLocked = false
                            Toast.makeText(context.applicationContext, "Digital autorizada!", Toast.LENGTH_SHORT).show()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Toast.makeText(context.applicationContext, "Digital não reconhecida. Tente de novo ou use a senha.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Login Agenda H")
                    .setSubtitle("Autentique usando sua impressão digital")
                    .setNegativeButtonText("Usar PIN / Senha")
                    .build()

                biometricPrompt.authenticate(promptInfo)
            } catch (e: Throwable) {
                Toast.makeText(context.applicationContext, "Hardware de biometria não disponível. Use o PIN.", Toast.LENGTH_LONG).show()
                lockScreenMode = "pin"
            }
        } else {
            Toast.makeText(context.applicationContext, "Erro interno de atividade. Use o PIN.", Toast.LENGTH_SHORT).show()
            lockScreenMode = "pin"
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.isBiometricsEnabled) {
            kotlinx.coroutines.delay(400)
            launchScreenBiometric()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgApp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (lockScreenMode == "select") {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Level1DarkSurface)
                    .border(1.5.dp, PrimaryGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PrimaryGold,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "AGENDA H",
                color = OnSurfaceGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Text(
                text = "Escolha uma forma de desbloqueio para entrar",
                color = OnSurfaceGoldVariant.copy(alpha = 0.8f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .border(1.5.dp, PrimaryGold.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .clickable { launchScreenBiometric() },
                    colors = CardDefaults.cardColors(containerColor = Level1DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PrimaryGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = PrimaryGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometria Digital",
                                color = OnSurfaceGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Acessar via impressão digital",
                                color = OnSurfaceGoldVariant,
                                fontSize = 11.sp
                            )
                        }
                        
                        Text(text = "→", color = PrimaryGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .clickable { lockScreenMode = "pin" },
                    colors = CardDefaults.cardColors(containerColor = Level1DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = OnSurfaceGoldVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Senha PIN",
                                color = OnSurfaceGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Digitar o código secreto",
                                color = OnSurfaceGoldVariant,
                                fontSize = 11.sp
                            )
                        }

                        Text(text = "→", color = OnSurfaceGoldVariant, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
            Text(
                text = "Se não for possível usar digital, toque em Senha PIN.",
                color = OnSurfaceGoldVariant.copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 48.dp)
            )

        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Level1DarkSurface)
                    .border(1.dp, PrimaryGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "AGENDA H", color = OnSurfaceGold, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(text = "Digite seu PIN de 4 dígitos", color = OnSurfaceGoldVariant, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in 0 until 4) {
                    val filled = i < codeTyped.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (filled) PrimaryGold else Color.LightGray.copy(alpha = 0.2f))
                            .border(1.dp, PrimaryGold, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    LockKeyButton(num = "1") { codeTyped = addNumChar(codeTyped, "1", correctCode, onFull = { viewModel.isAppLocked = false }, context) }
                    LockKeyButton(num = "2") { codeTyped = addNumChar(codeTyped, "2", correctCode, onFull = { viewModel.isAppLocked = false }, context) }
                    LockKeyButton(num = "3") { codeTyped = addNumChar(codeTyped, "3", correctCode, onFull = { viewModel.isAppLocked = false }, context) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    LockKeyButton(num = "4") { codeTyped = addNumChar(codeTyped, "4", correctCode, onFull = { viewModel.isAppLocked = false }, context) }
                    LockKeyButton(num = "5") { codeTyped = addNumChar(codeTyped, "5", correctCode, onFull = { viewModel.isAppLocked = false }, context) }
                    LockKeyButton(num = "6") { codeTyped = addNumChar(codeTyped, "6", correctCode, onFull = { viewModel.isAppLocked = false }, context) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    LockKeyButton(num = "7") { codeTyped = addNumChar(codeTyped, "7", correctCode, onFull = { viewModel.isAppLocked = false }, context) }
                    LockKeyButton(num = "8") { codeTyped = addNumChar(codeTyped, "8", correctCode, onFull = { viewModel.isAppLocked = false }, context) }
                    LockKeyButton(num = "9") { codeTyped = addNumChar(codeTyped, "9", correctCode, onFull = { viewModel.isAppLocked = false }, context) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(keyColor)
                            .clickable {
                                lockScreenMode = "select"
                                codeTyped = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar", tint = OnSurfaceGold)
                    }

                    LockKeyButton(num = "0") { codeTyped = addNumChar(codeTyped, "0", correctCode, onFull = { viewModel.isAppLocked = false }, context) }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(keyColor)
                            .clickable { if (codeTyped.isNotEmpty()) codeTyped = codeTyped.dropLast(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Backspace, contentDescription = "Limpar", tint = OnSurfaceGold)
                    }
                }
            }
        }
    }
}

private fun addNumChar(typedStr: String, c: String, correctCode: String, onFull: () -> Unit, context: android.content.Context): String {
    if (typedStr.length >= 4) return typedStr
    val next = typedStr + c
    if (next.length == 4) {
        if (next == correctCode) {
            onFull()
            return ""
        } else {
            Toast.makeText(context, "PIN Incorreto!", Toast.LENGTH_SHORT).show()
            return ""
        }
    }
    return next
}

@Composable
fun LockKeyButton(num: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Level1DarkSurface)
            .border(1.dp, Color.White.copy(alpha = 0.03f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = num, color = OnSurfaceGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

// ------------------- GLOBAL DIALOGS CREATIVE DETAILED FORMS -------------------

@Composable
fun AddOrEditWomanDialog(
    partner: Partner?,
    viewModel: AgendaViewModel,
    onDismiss: () -> Unit
) {
    val allEncontros by viewModel.allEncontros.collectAsStateWithLifecycle(initialValue = emptyList())
    var name by remember { mutableStateOf(partner?.name ?: "") }
    var ageStr by remember { mutableStateOf(partner?.age?.toString() ?: "25") }
    var childrenSelection by remember { mutableStateOf(partner?.children?.toString() ?: "0") }
    var myKidsSelection by remember { mutableStateOf(partner?.myKids?.toString() ?: "0") }
    var originSelection by remember { mutableStateOf(partner?.origin ?: "Tinder") }
    var objectiveSelection by remember { mutableStateOf(partner?.objective ?: "Casual") }
    var phoneStr by remember { mutableStateOf(partner?.phone ?: "") }
    var addressStr by remember { mutableStateOf(partner?.address ?: "") }
    var notes by remember { mutableStateOf(partner?.notes ?: "") }
    
    // Availability
    var availabilitySelection by remember { mutableStateOf(partner?.availability ?: "Solteira") }

    // Dropdown expanded states
    var childrenExpanded by remember { mutableStateOf(false) }
    var myKidsExpanded by remember { mutableStateOf(false) }
    var originExpanded by remember { mutableStateOf(false) }
    var objectiveExpanded by remember { mutableStateOf(false) }
    var availabilityDropdownExpanded by remember { mutableStateOf(false) }
    
    // Encontro / Sexo Inline Register
    var registeredMeeting by remember { mutableStateOf(false) }
    var meetingDate by remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        mutableStateOf(sdf.format(Date()))
    }
    val initialMotelCostStr = remember(partner, allEncontros) {
        val partnerId = partner?.id ?: 0
        val nonGpPartnerIds = viewModel.allPartners.value.filter { !it.isGP }.map { it.id }.toSet()
        if (partnerId > 0) {
            val partnerEncs = allEncontros.filter { it.partnerId == partnerId }
            if (partnerEncs.isNotEmpty()) {
                val lastEnc = partnerEncs.sortedByDescending { it.date }.first()
                val costVal = lastEnc.motelCost
                if (costVal % 1.0 == 0.0) costVal.toInt().toString() else costVal.toString()
            } else {
                val lastGlobalEnc = allEncontros.filter { nonGpPartnerIds.contains(it.partnerId) }.sortedByDescending { it.date }.firstOrNull() ?: allEncontros.sortedByDescending { it.date }.firstOrNull()
                if (lastGlobalEnc != null) {
                    val costVal = lastGlobalEnc.motelCost
                    if (costVal % 1.0 == 0.0) costVal.toInt().toString() else costVal.toString()
                } else {
                    "0"
                }
            }
        } else {
            val lastGlobalEnc = allEncontros.filter { nonGpPartnerIds.contains(it.partnerId) }.sortedByDescending { it.date }.firstOrNull() ?: allEncontros.sortedByDescending { it.date }.firstOrNull()
            if (lastGlobalEnc != null) {
                val costVal = lastGlobalEnc.motelCost
                if (costVal % 1.0 == 0.0) costVal.toInt().toString() else costVal.toString()
            } else {
                "0"
            }
        }
    }
    var motelCostStr by remember(initialMotelCostStr) {
        mutableStateOf(initialMotelCostStr)
    }
    var hadSex by remember { mutableStateOf(false) }
    var isVirginityLost by remember { mutableStateOf(false) }
    
    // Repeatable counts
    var countVaginal by remember { mutableStateOf(1) }
    var countAnal by remember { mutableStateOf(0) }
    var countCreampie by remember { mutableStateOf(0) }
    var countAnalCreampie by remember { mutableStateOf(0) }
    var countOral by remember { mutableStateOf(1) }
    var countFacial by remember { mutableStateOf(0) }
    var countSquirt by remember { mutableStateOf(0) }
    var countDeepthroat by remember { mutableStateOf(0) }
    var countPregnant by remember { mutableStateOf(0) }
    var isPregnancy by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf(5) } // default 5 stars

    // Collapsible sections
    var isAvailabilitySectionExpanded by remember { mutableStateOf(false) }
    var isNegativeSectionExpanded by remember { mutableStateOf(false) }
    
    // Selected negatives list
    val selectedNegatives = remember {
        mutableStateListOf<String>().apply {
            if (partner != null && partner.negatives.isNotEmpty()) {
                addAll(partner.negatives.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            }
        }
    }
    
    val negativeGroups = listOf(
        Pair("Vícios 🚫", listOf("fumante", "drogas", "bebebora", "jogadora")),
        Pair("Saúde 🏥", listOf("obesa", "endometriose", "crohn", "esteril", "dst", "mental", "pcd")),
        Pair("Comportamento 🧠", listOf("feminista", "ativista_lgbt", "esquerdista", "vegana", "instavel", "baladeira")),
        Pair("Situação 📋", listOf("familia", "foto", "ex_gp", "onlyfans", "desempregada", "dividas", "ex", "rodada", "religiosa"))
    )

    val context = LocalContext.current
    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
    val surfaceColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (viewModel.isDarkTheme) DeepDarkBackground else Color(0xFFFAF6F0),
            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (partner == null) "Nova Mulher 👩" else "Editar Cadastro",
                    color = PrimaryGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Nome / Apelido
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome / Apelido *") },
                    placeholder = { Text("Como você chama ela") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                )

                // Row: Idade, Filhos Dela (Outros) e Filhos Nossos (Meus)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = ageStr,
                        onValueChange = { ageStr = it },
                        label = { Text("Idade", fontSize = 10.sp) },
                        placeholder = { Text("24", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                    )

                    // Filhos Dela (Outros) Dropdown
                    Box(modifier = Modifier.weight(1.1f)) {
                        OutlinedButton(
                            onClick = { childrenExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f)),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(text = when (childrenSelection) {
                                "0" -> "Outros: 0"
                                "1" -> "Outros: 1"
                                "2" -> "Outros: 2"
                                "3" -> "Outros: 3"
                                "4" -> "Outros: 4+"
                                else -> "Outros: $childrenSelection"
                            }, fontSize = 11.sp, maxLines = 1)
                        }
                        DropdownMenu(
                            expanded = childrenExpanded,
                            onDismissRequest = { childrenExpanded = false },
                            modifier = Modifier.background(surfaceColor)
                        ) {
                            listOf("0", "1", "2", "3", "4").forEach { count ->
                                DropdownMenuItem(
                                    text = { Text(text = when (count) {
                                        "0" -> "Sem filhos"
                                        "1" -> "1 filho (outros)"
                                        "2" -> "2 filhos (outros)"
                                        "3" -> "3 filhos (outros)"
                                        "4" -> "4+ filhos (outros)"
                                        else -> "$count filhos"
                                    }, color = textColor, fontSize = 12.sp) },
                                    onClick = {
                                        childrenSelection = count
                                        childrenExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Filhos Meus (Nossos) Dropdown
                    Box(modifier = Modifier.weight(1.1f)) {
                        OutlinedButton(
                            onClick = { myKidsExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f)),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(text = when (myKidsSelection) {
                                "0" -> "Meus: 0"
                                "1" -> "Meus: 1"
                                "2" -> "Meus: 2"
                                "3" -> "Meus: 3"
                                "4" -> "Meus: 4+"
                                else -> "Meus: $myKidsSelection"
                            }, fontSize = 11.sp, maxLines = 1)
                        }
                        DropdownMenu(
                            expanded = myKidsExpanded,
                            onDismissRequest = { myKidsExpanded = false },
                            modifier = Modifier.background(surfaceColor)
                        ) {
                            listOf("0", "1", "2", "3", "4").forEach { count ->
                                DropdownMenuItem(
                                    text = { Text(text = when (count) {
                                        "0" -> "Sem nossos"
                                        "1" -> "1 filho (meu)"
                                        "2" -> "2 filhos (meus)"
                                        "3" -> "3 filhos (meus)"
                                        "4" -> "4+ filhos (meus)"
                                        else -> "$count de nós"
                                    }, color = textColor, fontSize = 12.sp) },
                                    onClick = {
                                        myKidsSelection = count
                                        myKidsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Origem / App Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { originExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f))
                    ) {
                        Text(text = "Origem: $originSelection")
                    }
                    DropdownMenu(
                        expanded = originExpanded,
                        onDismissRequest = { originExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f).background(surfaceColor)
                    ) {
                        listOf("Tinder", "Badoo", "Bumble", "Jaumo", "Lovoo", "Happn", "Facebook Namoro", "Instagram", "WhatsApp", "Pessoalmente", "Outros").forEach { item ->
                            DropdownMenuItem(
                                text = { Text(text = item, color = textColor) },
                                onClick = {
                                    originSelection = item
                                    originExpanded = false
                                }
                            )
                        }
                    }
                }

                // Objetivo dela Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { objectiveExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f))
                    ) {
                        Text(text = "Objetivo dela: $objectiveSelection")
                    }
                    DropdownMenu(
                        expanded = objectiveExpanded,
                        onDismissRequest = { objectiveExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f).background(surfaceColor)
                    ) {
                        listOf("Relacionamento sério", "Casual", "Ver no que dá", "Amizade / só conversar", "Vende packs").forEach { item ->
                            DropdownMenuItem(
                                text = { Text(text = item, color = textColor) },
                                onClick = {
                                    objectiveSelection = item
                                    objectiveExpanded = false
                                }
                            )
                        }
                    }
                }

                // WhatsApp
                OutlinedTextField(
                    value = phoneStr,
                    onValueChange = { phoneStr = it },
                    label = { Text("WhatsApp *") },
                    placeholder = { Text("5511999999999") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                )

                // Cidade / Endereço
                OutlinedTextField(
                    value = addressStr,
                    onValueChange = { addressStr = it },
                    label = { Text("Cidade / Endereço *") },
                    placeholder = { Text("Bairro, cidade ou endereço para o Waze") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                )

                // ENCONTRO / SEXO / FETICHES (Conditional inline addition block)
                if (partner == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Já houve encontro?", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = registeredMeeting,
                                    onCheckedChange = { registeredMeeting = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = OnPrimaryGold, checkedTrackColor = PrimaryGold)
                                )
                            }

                            if (registeredMeeting) {
                                val inlineCalContext = LocalContext.current
                                val displayInlineDate = com.example.data.WomanCalculator.formatToBrazilianDate(meetingDate)
                                val showInlineDatePicker = {
                                    val calendar = Calendar.getInstance()
                                    if (meetingDate.contains("-")) {
                                        val parts = meetingDate.split("-")
                                        if (parts.size == 3) {
                                            val y = parts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                                            val m = (parts[1].toIntOrNull() ?: 1) - 1
                                            val d = parts[2].toIntOrNull() ?: 1
                                            calendar.set(y, m, d)
                                        }
                                    } else if (meetingDate.contains("/")) {
                                        val parts = meetingDate.split("/")
                                        if (parts.size == 3) {
                                            val d = parts[0].toIntOrNull() ?: 1
                                            val m = (parts[1].toIntOrNull() ?: 1) - 1
                                            val y = parts[2].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                                            calendar.set(y, m, d)
                                        }
                                    }
                                    android.app.DatePickerDialog(
                                        inlineCalContext,
                                        { _, year, month, dayOfMonth ->
                                            meetingDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showInlineDatePicker() }
                                ) {
                                    OutlinedTextField(
                                        value = displayInlineDate,
                                        onValueChange = { },
                                        readOnly = true,
                                        label = { Text("Data do encontro (Toque para alterar)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = false,
                                        trailingIcon = {
                                            IconButton(onClick = { showInlineDatePicker() }) {
                                                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Selecionar Data", tint = PrimaryGold)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = textColor,
                                            focusedBorderColor = PrimaryGold,
                                            unfocusedTextColor = textColor,
                                            unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                                            disabledTextColor = textColor,
                                            disabledBorderColor = textColor.copy(alpha = 0.5f),
                                            disabledLabelColor = textVariantColor
                                        )
                                    )
                                }

                                OutlinedTextField(
                                    value = motelCostStr,
                                    onValueChange = { motelCostStr = it },
                                    label = { Text("Preço do Motel (R$)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🔥 Rolou sexo?", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Switch(
                                        checked = hadSex,
                                        onCheckedChange = { hadSex = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = OnPrimaryGold, checkedTrackColor = PrimaryGold)
                                    )
                                }

                                if (hadSex) {
                                    Text(text = "⭐ Eventos Únicos", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isVirginityLost,
                                            onCheckedChange = { isVirginityLost = it },
                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryGold)
                                        )
                                        Text(text = "Perda da Virgindade", color = textColor, fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "🔄 Repetíveis", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                    // Inline Counter Adjusters
                                    val counters = listOf(
                                        Triple("Vaginal", countVaginal, { v: Int -> countVaginal = v }),
                                        Triple("Anal", countAnal, { v: Int -> countAnal = v }),
                                        Triple("Creampie", countCreampie, { v: Int -> countCreampie = v }),
                                        Triple("Anal Creampie", countAnalCreampie, { v: Int -> countAnalCreampie = v }),
                                        Triple("Oral", countOral, { v: Int -> countOral = v }),
                                        Triple("Facial", countFacial, { v: Int -> countFacial = v }),
                                        Triple("Squirt", countSquirt, { v: Int -> countSquirt = v }),
                                        Triple("Garganta profunda", countDeepthroat, { v: Int -> countDeepthroat = v }),
                                        Triple("Grávida", countPregnant, { v: Int -> countPregnant = v })
                                    )

                                    counters.forEach { (lbl, currVal, updateFn) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = lbl, color = textColor, fontSize = 12.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { updateFn((currVal - 1).coerceAtLeast(0)) }) {
                                                    Text(text = "−", color = PrimaryGold, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                                }
                                                Text(text = "$currVal", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                IconButton(onClick = { updateFn(currVal + 1) }) {
                                                    Text(text = "+", color = PrimaryGold, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🤰 Vou ser Papai!", color = textColor, fontSize = 12.sp)
                                        Switch(
                                            checked = isPregnancy,
                                            onCheckedChange = { isPregnancy = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = OnPrimaryGold, checkedTrackColor = Color.Magenta)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Nota do Encontro: $rating/10", color = textColor, fontSize = 12.sp)
                                        Slider(
                                            value = rating.toFloat(),
                                            onValueChange = { rating = it.toInt() },
                                            valueRange = 0f..10f,
                                            steps = 9,
                                            colors = SliderDefaults.colors(thumbColor = PrimaryGold, activeTrackColor = PrimaryGold),
                                            modifier = Modifier.fillMaxWidth(0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Observações Geras / Fetiches
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações Geras / fetiches") },
                    placeholder = { Text("Gostos, fetiches, detalhes...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                )

                // COLLAPSIBLE: Disponibilidade Accordion
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAvailabilitySectionExpanded = !isAvailabilitySectionExpanded }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "📍 Disponibilidade", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Status: $availabilitySelection", color = textVariantColor, fontSize = 10.sp)
                            }
                            Text(text = if (isAvailabilitySectionExpanded) "▲" else "▼", color = PrimaryGold, fontSize = 12.sp)
                        }

                        if (isAvailabilitySectionExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Solteira", "Casada", "Indisponível", "Mora longe", "Perdeu o contato").forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { availabilitySelection = item }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = availabilitySelection == item,
                                            onClick = { availabilitySelection = item },
                                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = item, color = textColor, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // COLLAPSIBLE: Coisas Desagradáveis (Traços Negativos)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isNegativeSectionExpanded = !isNegativeSectionExpanded }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "😣 Coisas Desagradáveis", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Penalidades marcadas: ${selectedNegatives.size}", color = textVariantColor, fontSize = 10.sp)
                            }
                            Text(text = if (isNegativeSectionExpanded) "▲" else "▼", color = PrimaryGold, fontSize = 12.sp)
                        }

                        if (isNegativeSectionExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                negativeGroups.forEach { (catLabel, traitIds) ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.6f)),
                                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.05f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = catLabel, color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            traitIds.forEach { tId ->
                                                val trait = WomanCalculator.negativeTraitsList.find { it.id == tId } ?: return@forEach
                                                val isChecked = selectedNegatives.contains(tId)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            if (isChecked) selectedNegatives.remove(tId) else selectedNegatives.add(tId)
                                                        }
                                                        .padding(vertical = 2.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = {
                                                            if (isChecked) selectedNegatives.remove(tId) else selectedNegatives.add(tId)
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = PrimaryGold)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(text = "${trait.icon} ${trait.label} (-${trait.penalty} pts)", color = textColor, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Row action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (partner != null) {
                        Button(
                            onClick = {
                                viewModel.deletePartner(partner)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f), contentColor = Color.Red),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                        ) {
                            Text(text = "Excluir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", color = textVariantColor)
                        }

                        Button(
                            onClick = {
                                if (name.trim().isEmpty()) {
                                    Toast.makeText(context, "Nome é obrigatório!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val negativesStr = selectedNegatives.distinct().filter { it.isNotEmpty() }.joinToString(",")
                                val parsedAge = ageStr.toIntOrNull() ?: partner?.age ?: 25
                                val parsedChildrenCount = childrenSelection.toIntOrNull() ?: partner?.children ?: 0

                                val targetPartner = Partner(
                                    id = partner?.id ?: 0,
                                    name = name.trim(),
                                    location = if (addressStr.trim().isEmpty()) "" else {
                                        val lastC = addressStr.trim().lastIndexOf(',')
                                        if (lastC != -1) addressStr.trim().substring(lastC + 1).trim() else addressStr.trim()
                                    },
                                    origin = originSelection,
                                    age = parsedAge,
                                    notes = notes.trim(),
                                    isFavorite = partner?.isFavorite ?: false,
                                    photoUrl = partner?.photoUrl ?: "",
                                    rating = partner?.rating ?: 7.0,
                                    children = parsedChildrenCount,
                                    myKids = myKidsSelection.toIntOrNull() ?: partner?.myKids ?: 0,
                                    objective = objectiveSelection,
                                    phone = phoneStr.trim(),
                                    address = addressStr.trim(),
                                    availability = availabilitySelection,
                                    status = partner?.status ?: "Ativa",
                                    negatives = negativesStr,
                                    isGP = false,
                                    gpPrice = 0.0,
                                    createdAt = partner?.createdAt ?: System.currentTimeMillis(),
                                    firstDate = partner?.firstDate ?: viewModel.getTodayDateString(),
                                    affinity = partner?.affinity ?: 50
                                )

                                viewModel.addPartnerAndGetId(targetPartner) { insertedId ->
                                    if (partner == null && registeredMeeting) {
                                        var encNotes = ""
                                        if (isVirginityLost) encNotes += " [Perda da virgindade]"
                                        viewModel.addEncontro(
                                            partnerId = insertedId.toInt(),
                                            date = meetingDate,
                                            time = "22:00",
                                            motelCost = motelCostStr.toDoubleOrNull() ?: 0.0,
                                            typeOral = countOral > 0,
                                            typeAnal = countAnal > 0,
                                            typeVaginal = countVaginal > 0,
                                            typeCreampie = countCreampie > 0,
                                            typeAnalCreampie = countAnalCreampie > 0,
                                            notes = encNotes,
                                            hadSex = hadSex,
                                            isPregnancy = isPregnancy,
                                            rating = rating.toDouble(),
                                            typeFacial = countFacial > 0,
                                            typeSquirt = countSquirt > 0,
                                            typeDeepthroat = countDeepthroat > 0,
                                            countOral = countOral,
                                            countAnal = countAnal,
                                            countVaginal = countVaginal,
                                            countCreampie = countCreampie,
                                            countFacial = countFacial,
                                            countSquirt = countSquirt,
                                            countDeepthroat = countDeepthroat,
                                            countAnalCreampie = countAnalCreampie
                                        )
                                    }
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddOrEditGPDialog(
    partner: Partner?,
    viewModel: AgendaViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(partner?.name ?: "") }
    var location by remember { mutableStateOf(partner?.location ?: "") }
    var city by remember { mutableStateOf(partner?.address ?: "") }
    var phoneStr by remember { mutableStateOf(partner?.phone ?: "") }
    var gpPriceStr by remember { mutableStateOf(partner?.gpPrice?.toInt()?.toString() ?: "300") }
    var notes by remember { mutableStateOf(partner?.notes ?: "") }

    // Meeting values (mandatory when registering a new GP)
    var meetingDate by remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        mutableStateOf(sdf.format(Date()))
    }
    var gpSexoVaginal by remember { mutableStateOf(true) }
    var gpOralSemCamisinha by remember { mutableStateOf(false) }
    var gpSexoAnal by remember { mutableStateOf(false) }
    var gpBeijoNaBoca by remember { mutableStateOf(false) }
    var gp69ComCamisinha by remember { mutableStateOf(false) }
    var gpMassagemErotica by remember { mutableStateOf(false) }
    var gpDominacaoBdsm by remember { mutableStateOf(false) }
    var gpBeijoGrego by remember { mutableStateOf(false) }
    var gpInversaoFetiche by remember { mutableStateOf(false) }
    var gpGargantaProfunda by remember { mutableStateOf(false) }
    var gpFetichePe by remember { mutableStateOf(false) }
    var gpFioTerra by remember { mutableStateOf(false) }

    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
    val surfaceColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (viewModel.isDarkTheme) DeepDarkBackground else Color(0xFFFAF6F0),
            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (partner == null) "Nova GP 💃" else "Editar Cadastro GP",
                    color = PrimaryGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Nome / Apelido *
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome / Apelido *") },
                    placeholder = { Text("Como se apresenta") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                )

                // Endereço
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("📍 Endereço (Bairro, Cidade)") },
                    placeholder = { Text("Ex: Av. Paulista, Bela Vista, São Paulo") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                )

                // WhatsApp (opcional)
                OutlinedTextField(
                    value = phoneStr,
                    onValueChange = { phoneStr = it },
                    label = { Text("WhatsApp (opcional)") },
                    placeholder = { Text("Ex: 5511999999999") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                )

                // Preço / Tarifa (R$) - Campo único de valor
                OutlinedTextField(
                    value = gpPriceStr,
                    onValueChange = { gpPriceStr = it },
                    label = { Text("Preço / Tarifa Paga (R$) *") },
                    placeholder = { Text("Ex: 300") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                )

                // ENCONTRO SEXUAL OBRIGATÓRIO (Apenas no novo cadastro de GP)
                if (partner == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "🔥", fontSize = 16.sp)
                                Text(
                                    text = "ENCONTRO SEXUAL (REGISTRO AUTOMÁTICO)",
                                    color = PrimaryGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            val gpInlineContext = LocalContext.current
                            val displayGPInlineDate = com.example.data.WomanCalculator.formatToBrazilianDate(meetingDate)
                            val showGPInlineDatePicker = {
                                val calendar = Calendar.getInstance()
                                if (meetingDate.contains("-")) {
                                    val parts = meetingDate.split("-")
                                    if (parts.size == 3) {
                                        val y = parts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                                        val m = (parts[1].toIntOrNull() ?: 1) - 1
                                        val d = parts[2].toIntOrNull() ?: 1
                                        calendar.set(y, m, d)
                                    }
                                } else if (meetingDate.contains("/")) {
                                    val parts = meetingDate.split("/")
                                    if (parts.size == 3) {
                                        val d = parts[0].toIntOrNull() ?: 1
                                        val m = (parts[1].toIntOrNull() ?: 1) - 1
                                        val y = parts[2].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                                        calendar.set(y, m, d)
                                    }
                                }
                                android.app.DatePickerDialog(
                                    gpInlineContext,
                                    { _, year, month, dayOfMonth ->
                                        meetingDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showGPInlineDatePicker() }
                            ) {
                                OutlinedTextField(
                                    value = displayGPInlineDate,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Data do encontro sexual (Toque para alterar)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    trailingIcon = {
                                        IconButton(onClick = { showGPInlineDatePicker() }) {
                                            Icon(imageVector = Icons.Default.DateRange, contentDescription = "Selecionar Data", tint = PrimaryGold)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textColor,
                                        focusedBorderColor = PrimaryGold,
                                        unfocusedTextColor = textColor,
                                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                                        disabledTextColor = textColor,
                                        disabledBorderColor = textColor.copy(alpha = 0.5f),
                                        disabledLabelColor = textVariantColor
                                    )
                                )
                            }

                            Text(text = "✔ Serviços que rolaram:", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            CheckboxRow(label = "🔞 Sexo Vaginal", checked = gpSexoVaginal, onCheckedChange = { gpSexoVaginal = it })
                            CheckboxRow(label = "👄 Oral sem Camisinha", checked = gpOralSemCamisinha, onCheckedChange = { gpOralSemCamisinha = it })
                            CheckboxRow(label = "🍑 Sexo Anal", checked = gpSexoAnal, onCheckedChange = { gpSexoAnal = it })
                            CheckboxRow(label = "💋 Beijo na Boca", checked = gpBeijoNaBoca, onCheckedChange = { gpBeijoNaBoca = it })
                            CheckboxRow(label = "🔄 69 com Camisinha", checked = gp69ComCamisinha, onCheckedChange = { gp69ComCamisinha = it })
                            CheckboxRow(label = "💆‍♀️ Massagem Erótica", checked = gpMassagemErotica, onCheckedChange = { gpMassagemErotica = it })
                            CheckboxRow(label = "⛓️ Dominação / BDSM", checked = gpDominacaoBdsm, onCheckedChange = { gpDominacaoBdsm = it })
                            CheckboxRow(label = "🍯 Beijo Grego", checked = gpBeijoGrego, onCheckedChange = { gpBeijoGrego = it })
                            CheckboxRow(label = "🔄 Inversão de Fetiche", checked = gpInversaoFetiche, onCheckedChange = { gpInversaoFetiche = it })
                            CheckboxRow(label = "👅 Garganta Profunda", checked = gpGargantaProfunda, onCheckedChange = { gpGargantaProfunda = it })
                            CheckboxRow(label = "👣 Fetiche de Pé", checked = gpFetichePe, onCheckedChange = { gpFetichePe = it })
                            CheckboxRow(label = "☝️ Fio Terra", checked = gpFioTerra, onCheckedChange = { gpFioTerra = it })
                        }
                    }
                }

                // Observações - Campo único
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações") },
                    placeholder = { Text("Notas importantes, fetiches, detalhes...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                )

                // Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (partner != null) {
                        Button(
                            onClick = {
                                viewModel.deletePartner(partner)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f), contentColor = Color.Red),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                        ) {
                            Text(text = "Excluir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", color = textVariantColor)
                        }

                        Button(
                            onClick = {
                                if (name.trim().isEmpty()) {
                                    Toast.makeText(context, "Nome é obrigatório!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val parsedPrice = gpPriceStr.toDoubleOrNull() ?: partner?.gpPrice ?: 300.0

                                val targetGP = Partner(
                                    id = partner?.id ?: 0,
                                    name = name.trim(),
                                    location = location.trim(),
                                    origin = "Safe",
                                    age = partner?.age ?: 23,
                                    notes = notes.trim(),
                                    isGP = true,
                                    gpPrice = parsedPrice,
                                    address = city.trim(),
                                    phone = phoneStr.trim(),
                                    createdAt = partner?.createdAt ?: System.currentTimeMillis(),
                                    isFavorite = partner?.isFavorite ?: false,
                                    affinity = partner?.affinity ?: 50,
                                    rating = partner?.rating ?: 5.0,
                                    firstDate = partner?.firstDate ?: ""
                                )

                                if (partner == null) {
                                    // Nova GP: Sempre registra o encontro sexual obrigatoriamente
                                    viewModel.addPartnerAndGetId(targetGP) { insertedId ->
                                        viewModel.addEncontro(
                                            partnerId = insertedId.toInt(),
                                            date = meetingDate,
                                            time = "22:00",
                                            motelCost = parsedPrice,
                                            typeOral = false,
                                            typeAnal = false,
                                            typeVaginal = false,
                                            typeCreampie = false,
                                            notes = notes.trim(),
                                            hadSex = true,
                                            isPregnancy = false,
                                            rating = 8.0,
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
                                            gpGastoValor = parsedPrice
                                        )
                                    }
                                } else {
                                    // Editando perfil de GP já existente
                                    viewModel.updatePartner(targetGP)
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddGPEncontroDialog(
    gp: Partner,
    viewModel: AgendaViewModel,
    onDismiss: () -> Unit
) {
    var meetingDate by remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        mutableStateOf(sdf.format(Date()))
    }
    var priceStr by remember {
        mutableStateOf(if (gp.gpPrice > 0) gp.gpPrice.toInt().toString() else "300")
    }
    var gpSexoVaginal by remember { mutableStateOf(true) }
    var gpOralSemCamisinha by remember { mutableStateOf(false) }
    var gpSexoAnal by remember { mutableStateOf(false) }
    var gpBeijoNaBoca by remember { mutableStateOf(false) }
    var gp69ComCamisinha by remember { mutableStateOf(false) }
    var gpMassagemErotica by remember { mutableStateOf(false) }
    var gpDominacaoBdsm by remember { mutableStateOf(false) }
    var gpBeijoGrego by remember { mutableStateOf(false) }
    var gpInversaoFetiche by remember { mutableStateOf(false) }
    var gpGargantaProfunda by remember { mutableStateOf(false) }
    var gpFetichePe by remember { mutableStateOf(false) }
    var gpFioTerra by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (viewModel.isDarkTheme) DeepDarkBackground else Color(0xFFFAF6F0),
            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Novo Encontro 🔞 — ${gp.name}",
                    color = PrimaryGold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                // Date Picker
                val displayDate = com.example.data.WomanCalculator.formatToBrazilianDate(meetingDate)
                val showDatePicker = {
                    val calendar = Calendar.getInstance()
                    if (meetingDate.contains("-")) {
                        val parts = meetingDate.split("-")
                        if (parts.size == 3) {
                            val y = parts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                            val m = (parts[1].toIntOrNull() ?: 1) - 1
                            val d = parts[2].toIntOrNull() ?: 1
                            calendar.set(y, m, d)
                        }
                    } else if (meetingDate.contains("/")) {
                        val parts = meetingDate.split("/")
                        if (parts.size == 3) {
                            val d = parts[0].toIntOrNull() ?: 1
                            val m = (parts[1].toIntOrNull() ?: 1) - 1
                            val y = parts[2].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                            calendar.set(y, m, d)
                        }
                    }
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            meetingDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker() }
                ) {
                    OutlinedTextField(
                        value = displayDate,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Data do Encontro (Toque para alterar)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker() }) {
                                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Selecionar Data", tint = PrimaryGold)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = textColor,
                            disabledBorderColor = textColor.copy(alpha = 0.5f),
                            disabledLabelColor = textVariantColor
                        )
                    )
                }

                // Price (single field)
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Preço / Valor Pago (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                )

                Text(text = "✔ Serviços que rolaram:", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                CheckboxRow(label = "🔞 Sexo Vaginal", checked = gpSexoVaginal, onCheckedChange = { gpSexoVaginal = it })
                CheckboxRow(label = "👄 Oral sem Camisinha", checked = gpOralSemCamisinha, onCheckedChange = { gpOralSemCamisinha = it })
                CheckboxRow(label = "🍑 Sexo Anal", checked = gpSexoAnal, onCheckedChange = { gpSexoAnal = it })
                CheckboxRow(label = "💋 Beijo na Boca", checked = gpBeijoNaBoca, onCheckedChange = { gpBeijoNaBoca = it })
                CheckboxRow(label = "🔄 69 com Camisinha", checked = gp69ComCamisinha, onCheckedChange = { gp69ComCamisinha = it })
                CheckboxRow(label = "💆‍♀️ Massagem Erótica", checked = gpMassagemErotica, onCheckedChange = { gpMassagemErotica = it })
                CheckboxRow(label = "⛓️ Dominação / BDSM", checked = gpDominacaoBdsm, onCheckedChange = { gpDominacaoBdsm = it })
                CheckboxRow(label = "🍯 Beijo Grego", checked = gpBeijoGrego, onCheckedChange = { gpBeijoGrego = it })
                CheckboxRow(label = "🔄 Inversão de Fetiche", checked = gpInversaoFetiche, onCheckedChange = { gpInversaoFetiche = it })
                CheckboxRow(label = "👅 Garganta Profunda", checked = gpGargantaProfunda, onCheckedChange = { gpGargantaProfunda = it })
                CheckboxRow(label = "👣 Fetiche de Pé", checked = gpFetichePe, onCheckedChange = { gpFetichePe = it })
                CheckboxRow(label = "☝️ Fio Terra", checked = gpFioTerra, onCheckedChange = { gpFioTerra = it })

                // Notes (single field)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações do Encontro") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = textVariantColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsedPrice = priceStr.toDoubleOrNull() ?: gp.gpPrice
                            viewModel.addEncontro(
                                partnerId = gp.id,
                                date = meetingDate,
                                time = "22:00",
                                motelCost = parsedPrice,
                                typeOral = false,
                                typeAnal = false,
                                typeVaginal = false,
                                typeCreampie = false,
                                notes = notes.trim(),
                                hadSex = true,
                                isPregnancy = false,
                                rating = 8.0,
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
                                gpGastoValor = parsedPrice
                            )
                            Toast.makeText(context, "Encontro com ${gp.name} registrado! 🔥", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Salvar Encontro")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEncontroDialog(
    viewModel: AgendaViewModel,
    onDismiss: () -> Unit
) {
    val partnersList by viewModel.allPartners.collectAsStateWithLifecycle()
    val allEncontros by viewModel.allEncontros.collectAsStateWithLifecycle()
    val preselectedPartnerId = viewModel.preselectedPartnerIdForEncontro

    var selectedPartnerId by remember { 
        mutableStateOf(if (preselectedPartnerId > 0) preselectedPartnerId else (partnersList.firstOrNull()?.id ?: 0)) 
    }
    
    var date by remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        mutableStateOf(sdf.format(Date()))
    }
    var time by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var ratingStr by remember { mutableStateOf("10.0") }
    var hadSex by remember { mutableStateOf(true) }
    var motelCostStr by remember(selectedPartnerId, allEncontros) {
        val partnerEncs = allEncontros.filter { it.partnerId == selectedPartnerId }
        val nonGpPartnerIds = partnersList.filter { !it.isGP }.map { it.id }.toSet()
        val suggestedCost = if (partnerEncs.isNotEmpty()) {
            val lastEnc = partnerEncs.sortedByDescending { it.date }.first()
            val costVal = lastEnc.motelCost
            if (costVal % 1.0 == 0.0) costVal.toInt().toString() else costVal.toString()
        } else {
            val lastGlobalEnc = allEncontros.filter { nonGpPartnerIds.contains(it.partnerId) }.sortedByDescending { it.date }.firstOrNull() ?: allEncontros.sortedByDescending { it.date }.firstOrNull()
            if (lastGlobalEnc != null) {
                val costVal = lastGlobalEnc.motelCost
                if (costVal % 1.0 == 0.0) costVal.toInt().toString() else costVal.toString()
            } else {
                "0"
            }
        }
        mutableStateOf(suggestedCost)
    }

    // Mulheres attributes
    var typeVaginal by remember { mutableStateOf(true) }
    var countVaginal by remember { mutableStateOf(1) }
    var typeAnal by remember { mutableStateOf(false) }
    var countAnal by remember { mutableStateOf(1) }
    var typeCreampie by remember { mutableStateOf(false) }
    var countCreampie by remember { mutableStateOf(1) }
    var typeOral by remember { mutableStateOf(false) }
    var countOral by remember { mutableStateOf(1) }
    var typeFacial by remember { mutableStateOf(false) }
    var countFacial by remember { mutableStateOf(1) }
    var typeSquirt by remember { mutableStateOf(false) }
    var countSquirt by remember { mutableStateOf(1) }
    var typeDeepthroat by remember { mutableStateOf(false) }
    var countDeepthroat by remember { mutableStateOf(1) }
    
    var isFirstEncontroSex by remember { mutableStateOf(false) }
    var isVirginityLost by remember { mutableStateOf(false) }
    var typeAnalCreampie by remember { mutableStateOf(false) }
    var isPregnancyMarked by remember { mutableStateOf(false) }
    var countAnalCreampie by remember { mutableStateOf(1) }

    val partnerEncounters = allEncontros.filter { it.partnerId == selectedPartnerId }
    val alreadyHadFirstSex = partnerEncounters.any { it.isFirstEncontroSex || it.typeFirstEncounter }
    val alreadyLostVirginity = partnerEncounters.any { it.isVirginityLost || it.typeVirginity }

    // GP exclusive attributes
    var gpSexoVaginal by remember { mutableStateOf(false) }
    var gpOralSemCamisinha by remember { mutableStateOf(false) }
    var gpSexoAnal by remember { mutableStateOf(false) }
    var gpBeijoNaBoca by remember { mutableStateOf(false) }
    var gp69ComCamisinha by remember { mutableStateOf(false) }
    var gpMassagemErotica by remember { mutableStateOf(false) }
    var gpDominacaoBdsm by remember { mutableStateOf(false) }
    var gpBeijoGrego by remember { mutableStateOf(false) }
    var gpInversaoFetiche by remember { mutableStateOf(false) }
    var gpGargantaProfunda by remember { mutableStateOf(false) }
    var gpFetichePe by remember { mutableStateOf(false) }
    var gpFioTerra by remember { mutableStateOf(false) }
    var gpGastoValorStr by remember { mutableStateOf("") }

    var expandedDropdown by remember { mutableStateOf(false) }

    val selectedPartner = partnersList.find { it.id == selectedPartnerId }
    val isGP = selectedPartner?.isGP == true

    Dialog(onDismissRequest = onDismiss) {
        val dlgBgColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White
        val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
        val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
        val dlgBorderStroke = BorderStroke(1.dp, if (viewModel.isDarkTheme) PrimaryGold.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f))
        val dlgButtonBorderStroke = BorderStroke(1.dp, if (viewModel.isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.15f))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 14.dp),
            shape = RoundedCornerShape(16.dp),
            color = dlgBgColor,
            border = dlgBorderStroke
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isGP) "Registrar Conversão / Atendimento GP" else "Registrar Encontro Afetivo",
                    color = PrimaryGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Partner Spinner Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    val selectionName = selectedPartner?.name ?: "Selecione..."
                    OutlinedButton(
                        onClick = { expandedDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                        border = dlgButtonBorderStroke
                    ) {
                        Text(text = "Ficha selecionada: $selectionName")
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(dlgBgColor)
                    ) {
                        partnersList.forEach { partner ->
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = partner.name, color = textColor)
                                        Text(
                                            text = if (partner.isGP) "GP" else "Mulher", 
                                            color = if (partner.isGP) Color(0xFFEF5350) else PrimaryGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                onClick = {
                                    selectedPartnerId = partner.id
                                    expandedDropdown = false
                                    // Prepopulate/reset some states
                                    if (partner.isGP) {
                                        gpGastoValorStr = partner.gpPrice.toString()
                                        gpSexoVaginal = true
                                    }
                                }
                            )
                        }
                    }
                }

                val calendarContext = LocalContext.current
                val displayDate = com.example.data.WomanCalculator.formatToBrazilianDate(date)
                val showDatePicker = {
                    val calendar = Calendar.getInstance()
                    if (date.contains("-")) {
                        val parts = date.split("-")
                        if (parts.size == 3) {
                            val y = parts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                            val m = (parts[1].toIntOrNull() ?: 1) - 1
                            val d = parts[2].toIntOrNull() ?: 1
                            calendar.set(y, m, d)
                        }
                    } else if (date.contains("/")) {
                        val parts = date.split("/")
                        if (parts.size == 3) {
                            val d = parts[0].toIntOrNull() ?: 1
                            val m = (parts[1].toIntOrNull() ?: 1) - 1
                            val y = parts[2].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                            calendar.set(y, m, d)
                        }
                    }
                    android.app.DatePickerDialog(
                        calendarContext,
                        { _, year, month, dayOfMonth ->
                            date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker() }
                ) {
                    OutlinedTextField(
                        value = displayDate,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Data do Encontro (Toque para alterar)", color = textVariantColor) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker() }) {
                                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Selecionar Data", tint = PrimaryGold)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            focusedBorderColor = PrimaryGold,
                            unfocusedTextColor = textColor,
                            unfocusedBorderColor = textVariantColor.copy(alpha = 0.5f),
                            disabledTextColor = textColor,
                            disabledBorderColor = textVariantColor.copy(alpha = 0.5f),
                            disabledLabelColor = textVariantColor
                        )
                    )
                }

                // Rating
                OutlinedTextField(
                    value = ratingStr,
                    onValueChange = { ratingStr = it },
                    label = { Text(if (isGP) "Nota do Atendimento (0.0 - 10.0)" else "Nota do Encontro (0.0 - 10.0)", color = textVariantColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, focusedBorderColor = PrimaryGold, unfocusedTextColor = textColor, unfocusedBorderColor = textVariantColor.copy(alpha = 0.5f))
                )

                if (!isGP) {
                    // ---- MULHERES FLOW ----
                    OutlinedTextField(
                        value = motelCostStr,
                        onValueChange = { motelCostStr = it },
                        label = { Text("Preço do Motel (R$)", color = textVariantColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, focusedBorderColor = PrimaryGold, unfocusedTextColor = textColor, unfocusedBorderColor = textVariantColor.copy(alpha = 0.5f))
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Rolou Prática de Sexo ?", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = hadSex,
                            onCheckedChange = { hadSex = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OnPrimaryGold, checkedTrackColor = PrimaryGold)
                        )
                    }

                    if (hadSex) {
                        Text(text = "Fetiches e repetições:", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FetishRow(
                                label = "🔥 Sexo vaginal",
                                emoji = "🔥",
                                subtitleChecked = "Vaginal no encontro",
                                checked = typeVaginal,
                                onCheckedChange = { typeVaginal = it },
                                count = countVaginal,
                                onCountChange = { countVaginal = it }
                            )

                            FetishRow(
                                label = "🍑 Sexo anal",
                                emoji = "🍑",
                                subtitleChecked = "Anal no encontro",
                                checked = typeAnal,
                                onCheckedChange = { typeAnal = it },
                                count = countAnal,
                                onCountChange = { countAnal = it }
                            )

                            FetishRow(
                                label = "💦 🍕 Creampie",
                                emoji = "💦 🍕",
                                subtitleChecked = "Creampie realizado",
                                checked = typeCreampie,
                                onCheckedChange = { typeCreampie = it },
                                count = countCreampie,
                                onCountChange = { countCreampie = it }
                            )

                            FetishRow(
                                label = "🍑💦 Anal Creampie",
                                emoji = "🍑💦",
                                subtitleChecked = "Anal creampie realizado",
                                checked = typeAnalCreampie,
                                onCheckedChange = { typeAnalCreampie = it },
                                count = countAnalCreampie,
                                onCountChange = { countAnalCreampie = it }
                            )

                            FetishRow(
                                label = "😛 Oral (ela -> mim)",
                                emoji = "😛",
                                subtitleChecked = "Oral realizado",
                                checked = typeOral,
                                onCheckedChange = { typeOral = it },
                                count = countOral,
                                onCountChange = { countOral = it }
                            )

                            FetishRow(
                                label = "💦 Facial",
                                emoji = "💦",
                                subtitleChecked = "Facial realizado",
                                checked = typeFacial,
                                onCheckedChange = { typeFacial = it },
                                count = countFacial,
                                onCountChange = { countFacial = it }
                            )

                            FetishRow(
                                label = "🌊 Squirt",
                                emoji = "🌊",
                                subtitleChecked = "Squirt realizado",
                                checked = typeSquirt,
                                onCheckedChange = { typeSquirt = it },
                                count = countSquirt,
                                onCountChange = { countSquirt = it }
                            )

                            FetishRow(
                                label = "👅 Garganta profunda",
                                emoji = "👅",
                                subtitleChecked = "Garganta profunda",
                                checked = typeDeepthroat,
                                onCheckedChange = { typeDeepthroat = it },
                                count = countDeepthroat,
                                onCountChange = { countDeepthroat = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Grandes Marcos Afetivos:", color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val isMotherOfOthers = selectedPartner != null && selectedPartner.children > 0
                            val virginityLocked = alreadyLostVirginity || isMotherOfOthers

                            CheckboxRow(
                                label = if (alreadyHadFirstSex) "Primeiro sexo do casal (Já Cadastrado 🔒)" else "Primeiro sexo do casal (Badge Primeiro Sexo)", 
                                checked = isFirstEncontroSex && !alreadyHadFirstSex, 
                                enabled = !alreadyHadFirstSex,
                                onCheckedChange = { isFirstEncontroSex = it }
                            )
                            CheckboxRow(
                                label = if (alreadyLostVirginity) "Perda da virgindade dela (Já Cadastrado 🔒)" else if (isMotherOfOthers) "Perda da virgindade dela (Bloqueada: Já possui filhos 🔒)" else "Perda da virgindade dela nesse encontro", 
                                checked = isVirginityLost && !virginityLocked, 
                                enabled = !virginityLocked,
                                onCheckedChange = { isVirginityLost = it }
                            )

                            CheckboxRow(
                                label = "Registrar Alerta Gravidez/Paternidade (\"Vou ser papai!\")", 
                                checked = isPregnancyMarked, 
                                onCheckedChange = { isPregnancyMarked = it }
                            )
                        }
                    }

                } else {
                    // ---- GP FLOW ----
                    Text(text = "Custo & Tarifa do Atendimento:", color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = gpGastoValorStr,
                        onValueChange = { gpGastoValorStr = it },
                        label = { Text("Gasto total com GP (R$)", color = textVariantColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, focusedBorderColor = PrimaryGold, unfocusedTextColor = textColor, unfocusedBorderColor = textVariantColor.copy(alpha = 0.5f))
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Fetiches / Serviços Profissionais Praticados:", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        CheckboxRow(label = "🔞 Sexo Vaginal", checked = gpSexoVaginal, onCheckedChange = { gpSexoVaginal = it })
                        CheckboxRow(label = "👄 Oral sem Camisinha", checked = gpOralSemCamisinha, onCheckedChange = { gpOralSemCamisinha = it })
                        CheckboxRow(label = "🍑 Sexo Anal", checked = gpSexoAnal, onCheckedChange = { gpSexoAnal = it })
                        CheckboxRow(label = "💋 Beijo na Boca", checked = gpBeijoNaBoca, onCheckedChange = { gpBeijoNaBoca = it })
                        CheckboxRow(label = "🔄 69 com Camisinha", checked = gp69ComCamisinha, onCheckedChange = { gp69ComCamisinha = it })
                        CheckboxRow(label = "💆‍♀️ Massagem Erótica", checked = gpMassagemErotica, onCheckedChange = { gpMassagemErotica = it })
                        CheckboxRow(label = "⛓️ Dominação / BDSM", checked = gpDominacaoBdsm, onCheckedChange = { gpDominacaoBdsm = it })
                        CheckboxRow(label = "🍯 Beijo Grego", checked = gpBeijoGrego, onCheckedChange = { gpBeijoGrego = it })
                        CheckboxRow(label = "🔄 Inversão de Fetiche", checked = gpInversaoFetiche, onCheckedChange = { gpInversaoFetiche = it })
                        CheckboxRow(label = "👅 Garganta Profunda", checked = gpGargantaProfunda, onCheckedChange = { gpGargantaProfunda = it })
                        CheckboxRow(label = "👣 Fetiche de Pé", checked = gpFetichePe, onCheckedChange = { gpFetichePe = it })
                        CheckboxRow(label = "☝️ Fio Terra", checked = gpFioTerra, onCheckedChange = { gpFioTerra = it })
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Anotações adicionais", color = textVariantColor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, focusedBorderColor = PrimaryGold, unfocusedTextColor = textColor, unfocusedBorderColor = textVariantColor.copy(alpha = 0.5f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = textVariantColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedPartnerId > 0) {
                                if (isGP) {
                                    // Save with GP specific fields
                                    val cost = gpGastoValorStr.toDoubleOrNull() ?: 0.0
                                    viewModel.addEncontro(
                                        partnerId = selectedPartnerId,
                                        date = date,
                                        time = time,
                                        motelCost = cost,
                                        typeOral = gpOralSemCamisinha,
                                        typeAnal = gpSexoAnal,
                                        typeVaginal = gpSexoVaginal,
                                        typeCreampie = gpBeijoNaBoca, // Map creampie checkbox state alternatively
                                        notes = notes,
                                        hadSex = true,
                                        isPregnancy = false, // GP is strictly forbidden from pregnancy markers
                                        rating = ratingStr.toDoubleOrNull() ?: 10.0,
                                        typeFacial = gp69ComCamisinha,
                                        typeSquirt = gpMassagemErotica,
                                        typeDeepthroat = gpDominacaoBdsm || gpGargantaProfunda,
                                        countOral = if (gpOralSemCamisinha) 1 else 0,
                                        countAnal = if (gpSexoAnal) 1 else 0,
                                        countVaginal = if (gpSexoVaginal) 1 else 0,
                                        countCreampie = if (gpBeijoNaBoca) 1 else 0,
                                        countFacial = if (gp69ComCamisinha) 1 else 0,
                                        countSquirt = if (gpMassagemErotica) 1 else 0,
                                        countDeepthroat = if (gpDominacaoBdsm || gpGargantaProfunda) 1 else 0,
                                        isFirstEncontroSex = false,
                                        isVirginityLost = false,
                                        typeAnalCreampie = false,
                                        isPregnancyMarked = false,
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
                                        gpGastoValor = cost
                                    )
                                } else {
                                    // Save with standard Women fields
                                    viewModel.addEncontro(
                                        partnerId = selectedPartnerId,
                                        date = date,
                                        time = time,
                                        motelCost = motelCostStr.toDoubleOrNull() ?: 0.0,
                                        typeOral = if (hadSex) typeOral else false,
                                        typeAnal = if (hadSex) typeAnal else false,
                                        typeVaginal = if (hadSex) typeVaginal else false,
                                        typeCreampie = if (hadSex) typeCreampie else false,
                                        notes = notes,
                                        hadSex = hadSex,
                                        isPregnancy = isPregnancyMarked,
                                        rating = ratingStr.toDoubleOrNull() ?: 10.0,
                                        typeFacial = if (hadSex) typeFacial else false,
                                        typeSquirt = if (hadSex) typeSquirt else false,
                                        typeDeepthroat = if (hadSex) typeDeepthroat else false,
                                        countOral = if (hadSex && typeOral) countOral else 0,
                                        countAnal = if (hadSex && typeAnal) countAnal else 0,
                                        countVaginal = if (hadSex && typeVaginal) countVaginal else 0,
                                        countCreampie = if (hadSex && typeCreampie) countCreampie else 0,
                                        countFacial = if (hadSex && typeFacial) countFacial else 0,
                                        countSquirt = if (hadSex && typeSquirt) countSquirt else 0,
                                        countDeepthroat = if (hadSex && typeDeepthroat) countDeepthroat else 0,
                                        isFirstEncontroSex = isFirstEncontroSex,
                                        isVirginityLost = isVirginityLost,
                                        typeAnalCreampie = typeAnalCreampie,
                                        isPregnancyMarked = isPregnancyMarked,
                                        countAnalCreampie = if (hadSex && typeAnalCreampie) countAnalCreampie else 0
                                    )
                                }
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}

@Composable
fun CheckboxRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = PrimaryGold)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label, 
            color = if (enabled) OnSurfaceGold else OnSurfaceGold.copy(alpha = 0.35f), 
            fontSize = 13.sp
        )
    }
}

@Composable
fun FetishRow(
    label: String,
    emoji: String,
    subtitleChecked: String,
    subtitleUnchecked: String = "Toque para marcar",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    count: Int,
    onCountChange: (Int) -> Unit
) {
    // Strip leading emojis and whitespace from the label to fix duplication
    var cleanLabel = label.trim()
    cleanLabel = cleanLabel.replaceFirst("^[^\\p{L}\\p{N}\\p{Ps}\\p{Pi}]+".toRegex(), "").trim()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryGold,
                uncheckedColor = Color.LightGray.copy(alpha = 0.4f),
                checkmarkColor = Color.White
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Emoji icon
        Text(text = emoji, fontSize = 22.sp)
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Label & Status Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanLabel, 
                color = OnSurfaceGold, 
                fontSize = 15.sp, 
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (checked) subtitleChecked else subtitleUnchecked, 
                color = if (checked) PrimaryGold else OnSurfaceGoldVariant.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Plus/Minus counters if checked
        if (checked) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Minus button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { if (count > 1) onCountChange(count - 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "−", color = OnSurfaceGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                // Count text value
                Text(
                    text = "$count",
                    color = PrimaryGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Plus button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onCountChange(count + 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", color = OnSurfaceGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ------------------- COMBINED TABS: MULHERES & GPs -------------------

@Composable
fun FichasTabbedScreen(viewModel: AgendaViewModel) {
    val selectedSubTab = viewModel.selectedSubTab
    val showGP = viewModel.mostrarGP

    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val cardBg = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White

    Column(modifier = Modifier.fillMaxSize()) {
        if (showGP) {
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = if (viewModel.isDarkTheme) DeepDarkBackground else Color(0xFFFAF6F0),
                contentColor = PrimaryGold
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { viewModel.selectedSubTab = 0 },
                    text = { Text("Mulheres (Cidadais)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { viewModel.selectedSubTab = 1 },
                    text = { Text("Garotas de Programa (GPs)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (!showGP || selectedSubTab == 0) {
                PartnersScreen(viewModel = viewModel)
            } else {
                GPListScreen(viewModel = viewModel)
            }
        }
    }
}

// ------------------- SCREEN: SEDUCTION COACH IA (COACH INVISÍVEL) -------------------

fun decodeSampledBitmapFromUri(context: android.content.Context, uri: android.net.Uri, reqWidth: Int = 1024, reqHeight: Int = 1024): android.graphics.Bitmap? {
    try {
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
        }
        
        var inSampleSize = 1
        val height = options.outHeight
        val width = options.outWidth
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        val decodeOptions = android.graphics.BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            android.graphics.BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        }
    } catch (e: Exception) {
        return null
    }
}

@Composable
fun SeductionCoachScreen(viewModel: AgendaViewModel) {
    val context = LocalContext.current
    var coachSubTab by remember { mutableStateOf(0) } // 0 = Conversas e Prints, 1 = Perfil / Descrição
    
    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris != null && uris.isNotEmpty()) {
            uris.forEach { uri ->
                viewModel.addCoachImage(uri)
            }
        }
    }

    val selectedUris = viewModel.selectedCoachImageUris
    
    val bitmapsList = remember(selectedUris) {
        selectedUris.mapNotNull { uri ->
            decodeSampledBitmapFromUri(context, uri)
        }
    }

    val profileLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris != null && uris.isNotEmpty()) {
            uris.forEach { uri ->
                viewModel.addMyProfileImage(uri)
            }
        }
    }

    val selectedMyProfileUris = viewModel.selectedMyProfileImageUris
    val profileBitmapsList = remember(selectedMyProfileUris) {
        selectedMyProfileUris.mapNotNull { uri ->
            decodeSampledBitmapFromUri(context, uri)
        }
    }

    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
    val cardBg = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(bottom = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Face, contentDescription = "IA", tint = PrimaryGold, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("COACH INVISÍVEL IA 🎯", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Respostas inteligentes, abridores criativos e táticas de sedução", color = textVariantColor, fontSize = 12.sp)
                }
            }
        }

        // Subtabs selector Row - Custom rounded buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (viewModel.isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF2ECE4), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Conversas 💬", "Meu Perfil 👤", "Diagnóstico 📊", "Assistente IA 🤖").forEachIndexed { index, title ->
                val isSelected = coachSubTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryGold else Color.Transparent)
                        .clickable { coachSubTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) OnPrimaryGold else textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // API Key settings section at the top of either
        var showApiKeyPanel by remember { mutableStateOf(false) }
        var isKeyVisible by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.6f)),
            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showApiKeyPanel = !showApiKeyPanel }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = PrimaryGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chave API do Gemini (gemini-flash-latest)",
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val headerBadgeColor = when (viewModel.apiKeyStatus) {
                            "CONNECTED" -> Color(0xFF4CAF50)
                            "TESTING" -> PrimaryGold
                            else -> Color(0xFFEF5350)
                        }
                        val headerBadgeText = when (viewModel.apiKeyStatus) {
                            "CONNECTED" -> "✅ Ativa"
                            "TESTING" -> "⏳ Testando..."
                            else -> "⚠️ Verificar"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(headerBadgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = headerBadgeText,
                                color = headerBadgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(text = if (showApiKeyPanel) "▲" else "▼", color = PrimaryGold, fontSize = 11.sp)
                    }
                }

                if (showApiKeyPanel) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "O app valida e conecta automaticamente ao Google Gemini (gemini-flash-latest). Ao colar ou alterar a chave, o status de funcionamento é testado e confirmado em tempo real abaixo:",
                            color = textVariantColor,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        OutlinedTextField(
                            value = viewModel.coachApiKeyInput,
                            onValueChange = { viewModel.coachApiKeyInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (isKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isKeyVisible) "Ocultar Chave" else "Mostrar Chave",
                                        tint = PrimaryGold
                                    )
                                }
                            },
                            placeholder = { Text("Cole sua chave AI Studio...", fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = PrimaryGold
                            ),
                            singleLine = true
                        )

                        // Status Banner in real-time
                        val statusBg = when (viewModel.apiKeyStatus) {
                            "CONNECTED" -> Color(0xFF4CAF50).copy(alpha = 0.12f)
                            "TESTING" -> PrimaryGold.copy(alpha = 0.12f)
                            else -> Color(0xFFEF5350).copy(alpha = 0.12f)
                        }
                        val statusBorder = when (viewModel.apiKeyStatus) {
                            "CONNECTED" -> Color(0xFF4CAF50).copy(alpha = 0.4f)
                            "TESTING" -> PrimaryGold.copy(alpha = 0.4f)
                            else -> Color(0xFFEF5350).copy(alpha = 0.4f)
                        }
                        val statusTextColor = when (viewModel.apiKeyStatus) {
                            "CONNECTED" -> Color(0xFF4CAF50)
                            "TESTING" -> PrimaryGold
                            else -> Color(0xFFEF5350)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = statusBg),
                            border = BorderStroke(1.dp, statusBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when (viewModel.apiKeyStatus) {
                                    "TESTING" -> {
                                        CircularProgressIndicator(
                                            color = PrimaryGold,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    "CONNECTED" -> {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Conectado",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Erro",
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = viewModel.apiKeyStatusMessage,
                                    color = statusTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.testApiKeyConnection() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("🔄 Testar Agora", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.coachApiKeyInput = com.example.data.GeminiApiClient.DEFAULT_FALLBACK_KEY
                                    Toast.makeText(context, "Chave padrão redefinida com sucesso!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold.copy(alpha = 0.15f), contentColor = PrimaryGold),
                                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("🔑 Restaurar Chave", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        when (coachSubTab) {
            0 -> {
            // ORIGINAL MENSAGENS & PRINTS SCREEN
            Text("PRINT DA CONVERSA / PERFIL DA MULHER 📱", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = if (selectedUris.isNotEmpty()) 0.6f else 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (selectedUris.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Add image", tint = PrimaryGold, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Adicionar Múltiplos Prints / Perfil", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Upload de fotos da conversa no Tinder/Whats e foto de perfil dela", color = textVariantColor, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Imagens selecionadas (${selectedUris.size})", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { viewModel.clearCoachImages() }) {
                                    Text("Limpar tudo", color = Color.Red, fontSize = 11.sp)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                bitmapsList.forEachIndexed { idx, bmp ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 110.dp, height = 160.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(8.dp))
                                    ) {
                                        androidx.compose.foundation.Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Print $idx",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeCoachImage(selectedUris[idx]) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                                .size(24.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Excluir", tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                                
                                // Slot to add more screenshots
                                Box(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 160.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f)), RoundedCornerShape(8.dp))
                                        .clickable { launcher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add More", tint = PrimaryGold, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Mais Fotos", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Paste conversation text layout
            Text("CONTEXTO OU CONVERSA COPIADA (OPCIONAL)", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            OutlinedTextField(
                value = viewModel.coachCustomPrompt,
                onValueChange = { viewModel.coachCustomPrompt = it },
                placeholder = { Text("Cole aqui o papo da garota ou digite o dilema dela. Ex: Ela respondeu meio fria dizendo que não gosta de baladas no feriado. Como convidar para um vinho?", fontSize = 12.sp, color = textVariantColor.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth().height(110.dp).background(cardBg, RoundedCornerShape(8.dp)),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
            )

            // Ready simulation cases
            Text("EXPERIMENTAR COM CASOS DE SIMULAÇÃO (PREVIEW RÁPIDO)", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SimulationChip(label = "Tinder papo travado ❄️", onClick = {
                    viewModel.coachCustomPrompt = "Oi! Vi seu perfil da faculdade de direito, achei interessante mas quase nunca abro esse aplicativo. Me passa seu Instagram? (Como responder desviando o Insta para pedir o WhatsApp de forma sedutora?)"
                    Toast.makeText(context, "Simulador Tinder Carregado!", Toast.LENGTH_SHORT).show()
                }, viewModel = viewModel)
                
                SimulationChip(label = "Bio Instagram Seletiva 🍇", onClick = {
                    viewModel.coachCustomPrompt = "26 anos, advogada, só vinhos no Jardins, sem tempo para enrolação. Como mandar um direct que desperte curiosidade na hora?"
                    Toast.makeText(context, "Simulador Bio Carregado!", Toast.LENGTH_SHORT).show()
                }, viewModel = viewModel)

                SimulationChip(label = "Gelo pós sexo 🕵️", onClick = {
                    viewModel.coachCustomPrompt = "Tivemos relações no último sábado no Lush e ela ficou meio sumida. Mandei um bom dia e ela só respondeu agora após 24h: 'Oi sumido!'. Qual a melhor provocação para instigar o próximo encontro?"
                    Toast.makeText(context, "Simulador Gelo Carregado!", Toast.LENGTH_SHORT).show()
                }, viewModel = viewModel)
            }

            // Action Trigger Button
            Button(
                onClick = {
                    viewModel.analyzeCoachPrint(context, bitmapsList, viewModel.coachCustomPrompt)
                },
                enabled = !viewModel.isCoachLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (viewModel.isCoachLoading) {
                    CircularProgressIndicator(color = OnPrimaryGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Invisivel Coach pensando...")
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Spark", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analisar Tom & Gerar Abordagens IA", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Custom AI result screen containers - SPLIT into Feedback and Suggested Responses
            val feedbackResult = viewModel.coachAnalysisResult
            val suggestedResponses = viewModel.coachSuggestedResponses
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

            if (feedbackResult.isNotEmpty()) {
                Text("ANÁLISE DE CONTEXTO & POSTURA 🎯", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                          text = feedbackResult,
                          color = textColor,
                          fontSize = 13.sp,
                          lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(feedbackResult))
                                Toast.makeText(context, "Análise de jogo copiada! 📋", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = textColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copiar Feedback do Coach", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (viewModel.coachDateStrategyResult.isNotEmpty()) {
                Text("ESTRATEGISTA DE DATE IA (ROTEIRO & ESCALADA ÍNTIMA) 📍", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.2.dp, PrimaryGold.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🍸", fontSize = 18.sp)
                                Text("Roteiro Tático do Date", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(viewModel.coachDateStrategyResult))
                                    Toast.makeText(context, "Estratégia de Date copiada! 📋", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar Roteiro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = viewModel.coachDateStrategyResult,
                            color = textColor,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            val optionsList = viewModel.coachSuggestedOptions
            if (optionsList.isNotEmpty()) {
                Text("3 SUGESTÕES DE RESPOSTAS (COPIAR & COLAR NO APP) 💬", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                optionsList.forEachIndexed { index, optionText ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.2.dp, PrimaryGold.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Opção ${index + 1} 🔥",
                                    color = PrimaryGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(optionText))
                                        Toast.makeText(context, "Opção ${index + 1} copiada! Cole no app de namoro 🔥", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copiar Opção ${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = optionText,
                                color = textColor,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                if (suggestedResponses.isNotEmpty()) {
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(suggestedResponses))
                            Toast.makeText(context, "Todas as 3 opções copiadas! 🔥", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold.copy(alpha = 0.15f), contentColor = PrimaryGold),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copiar Todas as 3 Respostas", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (suggestedResponses.isNotEmpty()) {
                Text("RESPOSTAS PRONTAS SUGERIDAS (COPIAR & COLAR) 💬", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.5.dp, PrimaryGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = suggestedResponses,
                            color = textColor,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(suggestedResponses))
                                Toast.makeText(context, "Respostas prontas copiadas com sucesso! 🔥", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold.copy(alpha = 0.25f), contentColor = PrimaryGold),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copiar Respostas", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        1 -> {
            // NEW TAB 2: PORTFOLIO & BIOGRAPHY ANALYZER / GENERATOR
            Text("MEUS ACESSÓRIOS / DETALHES PESSOAIS 🎯", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Forneça os dados reais do seu estilo de vida para que o gerador monte descrições autênticas de alta taxa de resposta.",
                        color = textVariantColor,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = viewModel.myHobbiesInput,
                        onValueChange = { viewModel.myHobbiesInput = it },
                        label = { Text("Minha Profissão, Esportes e Passatempos", fontSize = 11.sp) },
                        placeholder = { Text("Ex: Advogado corporativo, surf no final de semana, violão, curte vinhos italianos...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                    )

                    OutlinedTextField(
                        value = viewModel.myTargetInput,
                        onValueChange = { viewModel.myTargetInput = it },
                        label = { Text("Estilo de Mulher que Deseja Atrair", fontSize = 11.sp) },
                        placeholder = { Text("Ex: Mulheres intelectuais, de valor elevado, descontraídas ou ligadas a esportes...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                    )
                }
            }

            Text("ESTILO DE DESCRIÇÃO DESEJADO 🎭", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Misterioso & Provocador", "Engraçado & Irônico", "Direto & Alpha", "Estilo Storytelling").forEach { styleOption ->
                    val isSelectedStyle = viewModel.myDescriptionStyle == styleOption
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isSelectedStyle) PrimaryGold else cardBg)
                            .border(1.dp, PrimaryGold.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                            .clickable { viewModel.myDescriptionStyle = styleOption }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = styleOption,
                            color = if (isSelectedStyle) OnPrimaryGold else textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text("MINHAS FOTOS ATUAIS (OPCIONAL - PARA ANÁLISE DO COACH) 📸", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = if (selectedMyProfileUris.isNotEmpty()) 0.6f else 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (selectedMyProfileUris.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .clickable { profileLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Add image", tint = PrimaryGold, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Adicionar Minhas Fotos Atuais", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Nossa IA analisará ângulos, fundos e dirá qual a melhor combinação para conversão", color = textVariantColor, fontSize = 10.sp, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Minhas Fotos selecionadas (${selectedMyProfileUris.size})", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { viewModel.clearMyProfileImages() }) {
                                    Text("Limpar tudo", color = Color.Red, fontSize = 11.sp)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                profileBitmapsList.forEachIndexed { idx, bmp ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 100.dp, height = 140.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(8.dp))
                                    ) {
                                        androidx.compose.foundation.Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "My Photo $idx",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeMyProfileImage(selectedMyProfileUris[idx]) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                                .size(24.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Excluir", tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(width = 100.dp, height = 140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f)), RoundedCornerShape(8.dp))
                                        .clickable { profileLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add More", tint = PrimaryGold, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Mais Fotos", color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action button
            Button(
                onClick = {
                    viewModel.analyzeMyProfile(
                        context,
                        profileBitmapsList,
                        viewModel.myDescriptionStyle,
                        viewModel.myHobbiesInput,
                        viewModel.myTargetInput
                    )
                },
                enabled = !viewModel.isProfileLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (viewModel.isProfileLoading) {
                    CircularProgressIndicator(color = OnPrimaryGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Gerando Direcionamento Técnico...")
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Spark", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analisar Perfil & Gerar Descrições Táticas", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Copy helper
            val profileFeedback = viewModel.profileAnalysisResult
            val generatedBios = viewModel.generatedBiosResult
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

            if (profileFeedback.isNotEmpty()) {
                Text("PARECER DO COACH TIER 1 & DICAS DE FOTOS 📊", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                          text = profileFeedback,
                          color = textColor,
                          fontSize = 13.sp,
                          lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(profileFeedback))
                                Toast.makeText(context, "Dicas de fotos copiadas! 📋", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = textColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copiar Feedback de Fotos", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (generatedBios.isNotEmpty()) {
                Text("DESCRIÇÕES DE SUCESSO GERADAS 📝", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.5.dp, PrimaryGold)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = generatedBios,
                            color = textColor,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(generatedBios))
                                Toast.makeText(context, "Descrições prontas copiadas com sucesso! 🔥", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold.copy(alpha = 0.25f), contentColor = PrimaryGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copiar Descrições Prontas", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        2 -> {
            // SUBTAB 2: DIAGNÓSTICO ESTRATÉGICO DA AGENDA (GEMINI FLASH)
            Text("DIAGNÓSTICO ESTRATÉGICO DA AGENDA (GEMINI FLASH) 📊", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Análise Tática dos Dados da Agenda", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "O modelo Gemini Flash analisa todas as mulheres cadastradas, encontros sexuais, critérios de classificação (Futura Esposa, Namorada em Potencial, Ficante Assídua) e dias de abstinência sexual para gerar um parecer estratégico completo.",
                        color = textVariantColor,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Button(
                        onClick = { viewModel.runIntelligentAgendaAnalysis(context) },
                        enabled = !viewModel.isAgendaAnalysisLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (viewModel.isAgendaAnalysisLoading) {
                            CircularProgressIndicator(color = OnPrimaryGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processando diagnóstico com Gemini...", fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gerar Diagnóstico da Agenda", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            val agendaResult = viewModel.agendaAnalysisResult
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

            if (agendaResult.isNotEmpty()) {
                Text("PARECER ESTRATÉGICO GERADO 📋", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = agendaResult,
                            color = textColor,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(agendaResult))
                                    Toast.makeText(context, "Diagnóstico copiado com sucesso! 📋", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold.copy(alpha = 0.2f), contentColor = textColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar Diagnóstico", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.clearAgendaAnalysis() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Limpar", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        else -> {
            // SUBTAB 3: ASSISTENTE INTELIGENTE GEMINI FLASH
            Text("ASSISTENTE INTELIGENTE IA (GEMINI FLASH) 🤖", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Consultoria Direta de Relacionamentos", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Tire dúvidas pontuais sobre sedução, abridores de conversa, comportamento feminino ou peça orientações táticas sobre sua dinâmica de relacionamentos.",
                        color = textVariantColor,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Text("SUGESTÕES RÁPIDAS DE CONSULTA:", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Quem devo priorizar esta semana?",
                            "Como puxar papo após 30 dias de abstinência?",
                            "Melhor abridor no Whats para date casual?",
                            "Quais os sinais claros de futura esposa?"
                        ).forEach { promptSuggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(PrimaryGold.copy(alpha = 0.12f))
                                    .border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                                    .clickable {
                                        viewModel.assistantQuery = promptSuggestion
                                        viewModel.askGeminiAssistant(promptSuggestion)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = promptSuggestion, color = textColor, fontSize = 11.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.assistantQuery,
                        onValueChange = { viewModel.assistantQuery = it },
                        placeholder = { Text("Digite sua pergunta para o Assistente Gemini...", fontSize = 12.sp, color = textVariantColor.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                    )

                    Button(
                        onClick = { viewModel.askGeminiAssistant(viewModel.assistantQuery) },
                        enabled = !viewModel.isAssistantLoading && viewModel.assistantQuery.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (viewModel.isAssistantLoading) {
                            CircularProgressIndicator(color = OnPrimaryGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Consultando Assistente...", fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar Pergunta ao Gemini", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            val assistantAnswer = viewModel.assistantAnswer
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

            if (assistantAnswer.isNotEmpty()) {
                Text("RESPOSTA DO ASSISTENTE INTELIGENTE 💬", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = assistantAnswer,
                            color = textColor,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(assistantAnswer))
                                    Toast.makeText(context, "Resposta copiada! 📋", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold.copy(alpha = 0.2f), contentColor = textColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar Resposta", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.clearAssistantAnswer() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Limpar", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SimulationChip(
    label: String,
    onClick: () -> Unit,
    viewModel: AgendaViewModel
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (viewModel.isDarkTheme) Level1DarkSurface else Color.White)
            .border(1.dp, PrimaryGold.copy(alpha = 0.35f), RoundedCornerShape(100.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = label, color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ------------------- POPUP DIALOG: DETALHES DE PONTUAÇÃO, BADGES E CRITÉRIOS DA MULHER -------------------

fun requestAutomaticGPSLocation(context: android.content.Context, onLocationResult: (String) -> Unit) {
    requestAutomaticGPSLocationDetailed(context) { name, _, _ ->
        onLocationResult(name)
    }
}

fun requestAutomaticGPSLocationDetailed(
    context: android.content.Context,
    onResult: (name: String, lat: Double, lng: Double) -> Unit
) {
    val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    if (fineGranted || coarseGranted) {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
        if (locationManager != null) {
            Thread {
                try {
                    val gpsLocation = try { locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER) } catch (e: Exception) { null }
                    val netLocation = try { locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { null }
                    val finalLoc = gpsLocation ?: netLocation
                    if (finalLoc != null) {
                        val lat = finalLoc.latitude
                        val lng = finalLoc.longitude
                        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = try { geocoder.getFromLocation(lat, lng, 1) } catch (e: Exception) { null }
                        val city = if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val c = addr.subAdminArea ?: addr.locality ?: addr.subLocality ?: "Campinas"
                            val state = addr.adminArea ?: "SP"
                            "$c, $state"
                        } else {
                            "Campinas, SP"
                        }
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            onResult(city, lat, lng)
                        }
                    } else {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            onResult("Campinas, SP", -22.9064, -47.0616)
                        }
                    }
                } catch (e: Exception) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onResult("Campinas, SP", -22.9064, -47.0616)
                    }
                }
            }.start()
        } else {
            onResult("Campinas, SP", -22.9064, -47.0616)
        }
    } else {
        onResult("Campinas, SP", -22.9064, -47.0616)
    }
}

@Composable
fun WomanDetailDialog(
    partner: Partner,
    onDismiss: () -> Unit,
    viewModel: AgendaViewModel
) {
    val encounters by viewModel.allEncontros.collectAsStateWithLifecycle()
    val partnerEncounters = encounters.filter { it.partnerId == partner.id }

    val context = androidx.compose.ui.platform.LocalContext.current
    var myCurrentGPSByWaze by remember { mutableStateOf("Campinas, SP") }
    var showGPSTriggerPopup by remember { mutableStateOf(false) }

    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            requestAutomaticGPSLocation(context) { resolvedLoc ->
                myCurrentGPSByWaze = resolvedLoc
                android.widget.Toast.makeText(context, "GPS Localizado: $resolvedLoc", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "Permissão negada. Localização manual definida.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    var selectedTab by remember { mutableStateOf(0) }

    val textColor = if (viewModel.isDarkTheme) OnSurfaceGold else Color(0xFF4A3416)
    val textVariantColor = if (viewModel.isDarkTheme) OnSurfaceGoldVariant else Color(0xFF55524B)
    val surfaceColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White

    val scoreW = WomanCalculator.calculateScoreW(partner, partnerEncounters)
    val priority = WomanCalculator.calculatePriorityScore(partner, partnerEncounters)
    val affinity = WomanCalculator.calculateAffinityPercent(partner, partnerEncounters)
    val affinityLabel = WomanCalculator.calculateAffinityLabel(partner, partnerEncounters)
    val heat = WomanCalculator.calculateHeatScore(partner, partnerEncounters)
    val borderGroup = WomanCalculator.calculateBorderColorGroup(partner, partnerEncounters)

    val isMostRecentReg = viewModel.checkIsMostRecentSexGlobal(partner.id, encounters)
    val badges = WomanCalculator.calculateBadges(partner, partnerEncounters, isMostRecentReg)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = if (viewModel.isDarkTheme) DeepDarkBackground else Color(0xFFFAF6F0),
            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Block with Photo and Name information
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Level2DarkSurface)
                            .border(1.dp, PrimaryGold, CircleShape)
                    ) {
                        if (partner.photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = partner.photoUrl,
                                contentDescription = partner.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(partner.name.take(1).uppercase(), color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = partner.name, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${partner.origin} • ${partner.getCity()} • Idade ${WomanCalculator.calculateCurrentAge(partner)}",
                            color = textVariantColor.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Red.copy(alpha = 0.7f))
                    }
                }

                // Sub Header tab segments
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = surfaceColor,
                    contentColor = PrimaryGold
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Scores / Badges", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Critérios", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Sugestões de Date", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // Scrollable details segment container
                var dragAmount by remember { mutableStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragAmount > 100) {
                                        // Swipe right -> Go to previous tab
                                        if (selectedTab > 0) {
                                            selectedTab--
                                        }
                                    } else if (dragAmount < -100) {
                                        // Swipe left -> Go to next tab
                                        if (selectedTab < 2) {
                                            selectedTab++
                                        }
                                    }
                                    dragAmount = 0f
                                },
                                onHorizontalDrag = { _, dragAmountLocal ->
                                    dragAmount += dragAmountLocal
                                }
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedTab) {
                        0 -> {
                            Text("PONTUAÇÃO OPERACIONAL", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ScoreCardItem(modifier = Modifier.weight(1f), title = "Score Mulher W", value = "$scoreW", subtitle = "Critérios base", viewModel = viewModel)
                                ScoreCardItem(modifier = Modifier.weight(1f), title = "Prioridade H", value = "$priority", subtitle = "Filtro Atração", viewModel = viewModel)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ScoreCardItem(modifier = Modifier.weight(1f), title = "Afinidade %", value = "$affinity%", subtitle = affinityLabel, viewModel = viewModel)
                                ScoreCardItem(modifier = Modifier.weight(1f), title = "Temperatura H", value = heat, subtitle = "Status de papo", viewModel = viewModel)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Certification Group Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (colorVal, colorName) = when (borderGroup) {
                                        "Ouro" -> Pair(Color(0xFFFFD700), "Selo de Ouro (Elite)")
                                        "Prata" -> Pair(Color(0xFFC0C0C0), "Selo de Prata (Excelente)")
                                        "Bronze" -> Pair(Color(0xFFCD7F32), "Selo de Bronze (Padrão)")
                                        "Verde" -> Pair(Color(0xFF4CAF50), "Verde (Papo Quente - Sem Sexo)")
                                        "Amarelo" -> Pair(Color(0xFFFFEB3B), "Amarelo (Morno - Reativar)")
                                        "Vermelho" -> Pair(Color(0xFFF44336), "Vermelho (Contato Frio)")
                                        else -> Pair(Color.Gray, "Cinza (Arquivada)")
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(colorVal.copy(alpha = 0.25f))
                                            .border(1.5.dp, colorVal, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Grupo de Estabilidade H", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(colorName, color = textVariantColor, fontSize = 11.sp)
                                    }
                                }
                            }

                            // BADGES & CONQUISTAS Section
                            Text("BADGES & CONQUISTAS (${badges.size})", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            if (badges.isEmpty()) {
                                Text("Nenhum badge ativo no momento.", color = textVariantColor, fontSize = 11.sp)
                            } else {
                                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    badges.forEach { b ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (b.isNegative) Color(0x22EF5350)
                                                    else if (b.isRelationship) Color(0x33BBDEFB)
                                                    else if (b.isVolatile) Color(0x33FFB74D)
                                                    else PrimaryGold.copy(alpha = 0.12f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (b.isNegative) Color(0xFFEF5350).copy(alpha = 0.35f)
                                                    else if (b.isRelationship) Color(0xFF1E88E5).copy(alpha = 0.35f)
                                                    else if (b.isVolatile) Color(0xFFFFA726).copy(alpha = 0.35f)
                                                    else PrimaryGold.copy(alpha = 0.35f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(text = b.icon, fontSize = 16.sp)
                                                Text(
                                                    text = b.text,
                                                    color = if (b.isNegative) Color(0xFFE57373)
                                                    else if (b.isRelationship) Color(0xFF64B5F6)
                                                    else if (b.isVolatile) Color(0xFFFFB74D)
                                                    else OnSurfaceGold,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // TRAÇOS NEGATIVOS Section
                            val negList = partner.negatives.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            Text("⚠️ TRAÇOS NEGATIVOS (${negList.size})", color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            if (negList.isEmpty()) {
                                Text("Nenhum traço negativo ou redflag cadastrado. Perfil imaculado! ✨", color = textVariantColor, fontSize = 11.sp)
                            } else {
                                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    negList.forEach { pId ->
                                        val trait = WomanCalculator.negativeTraitsList.find { it.id == pId }
                                        val label = trait?.label ?: pId
                                        val icon = trait?.icon ?: "🚩"
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x15EF5350))
                                                .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(text = icon, fontSize = 14.sp)
                                                Text(
                                                    text = label,
                                                    color = Color(0xFFE57373),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        1 -> {
                            Text("PONTOS DE CRITÉRIOS AVALIADOS", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            
                            CriteriaValueItem(label = "Status", value = partner.status, icon = "📌", color = textColor)
                            CriteriaValueItem(label = "Disponibilidade", value = partner.availability, icon = "💍", color = textColor)
                            CriteriaValueItem(label = "Objetivo", value = partner.objective, icon = "🎯", color = textColor)
                            CriteriaValueItem(label = "Loc / Cidade", value = partner.getCity(), icon = "📍", color = textColor)
                            CriteriaValueItem(label = "Filhos dela", value = "${partner.children} filhos", icon = "👶", color = textColor)
                            CriteriaValueItem(label = "Nossos filhos", value = "${partner.myKids} filhos", icon = "🧬", color = textColor)
                            CriteriaValueItem(label = "Telefone", value = partner.phone.ifEmpty { "Não cadastrado" }, icon = "📞", color = textColor)
                            
                            val negList = partner.negatives.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            Text("PENALIDADES E REDFLAGS (${negList.size})", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            if (negList.isEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✅", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Nenhum traço negativo ou redflag cadastrado. Perfil imaculado!", color = textColor, fontSize = 12.sp)
                                }
                            } else {
                                negList.forEach { pId ->
                                    val trait = WomanCalculator.negativeTraitsList.find { it.id == pId }
                                    if (trait != null) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0x15EF5350)),
                                            border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.2f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = trait.icon, fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = trait.label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                Text(text = "-${trait.penalty} pts", color = Color(0xFFE57373), fontSize = 12.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            var selectedLocationFilter by remember { mutableStateOf("Casa dela") }
                            var selectedCategory by remember { mutableStateOf("Motel") }
                            var selectedSubCategory by remember { mutableStateOf<String?>(null) }
                            val context = LocalContext.current

                            // Coordinates or simulated GPS location
                            var showGPSModifierDialog by remember { mutableStateOf(false) }

                            // Dynamic resolution of city and reference address for calculations
                            val (locationAddress, myGPSLocation) = remember(selectedLocationFilter, myCurrentGPSByWaze, viewModel.userGpsAddress) {
                                val gps = if (viewModel.userGpsAddress.isNotBlank()) viewModel.userGpsAddress else myCurrentGPSByWaze
                                val addr = when (selectedLocationFilter) {
                                    "Minha localização" -> gps
                                    "Casa dela" -> {
                                        if (partner.address.isNotBlank()) partner.address else gps
                                    }
                                    else -> gps
                                }
                                Pair(addr, gps)
                            }

                            Button(
                                onClick = {
                                    viewModel.destinationPartnerId = partner.id
                                    viewModel.destinationLocationFilter = selectedLocationFilter
                                    viewModel.currentTab = MainTab.DATE_SUGGESTIONS
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(imageVector = Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Abrir no Módulo Completo de Sugestões (7 km)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🗺️ PLANEJAR SUGESTÃO DE DATE",
                                    color = PrimaryGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                
                                Text(
                                    text = "GPS: $myGPSLocation ⚙️",
                                    color = PrimaryGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PrimaryGold.copy(alpha = 0.1f))
                                        .clickable { showGPSModifierDialog = true }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (showGPSModifierDialog) {
                                Dialog(onDismissRequest = { showGPSModifierDialog = false }) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                        border = BorderStroke(1.dp, PrimaryGold)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text("Definir Localização do GPS", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            var tempGpsText by remember { mutableStateOf(myCurrentGPSByWaze) }
                                            OutlinedTextField(
                                                value = tempGpsText,
                                                onValueChange = { tempGpsText = it },
                                                label = { Text("Cidade / Bairro do GPS", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = PrimaryGold)
                                            )
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                TextButton(onClick = { showGPSModifierDialog = false }) {
                                                    Text("Cancelar", color = Color.Gray)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = {
                                                        if (tempGpsText.isNotBlank()) {
                                                            myCurrentGPSByWaze = tempGpsText
                                                            viewModel.setManualLocation(tempGpsText)
                                                        }
                                                        showGPSModifierDialog = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold)
                                                ) {
                                                    Text("Salvar")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Address badge alert
                            if (partner.address.isEmpty() && selectedLocationFilter != "Minha localização") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x15EF5350))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${partner.name} não possui endereço. Por favor edite o perfil para incluir.",
                                        color = Color(0xFFE57373),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 14.sp
                                    )
                                }
                            } else if (partner.address.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Home, contentDescription = "Home", tint = PrimaryGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Endereço dela: ${partner.address}",
                                        color = textVariantColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Location selectors list
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Casa dela", "Minha localização").forEach { filter ->
                                    val isSelected = selectedLocationFilter == filter
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(if (isSelected) PrimaryGold.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (isSelected) PrimaryGold else Color.Gray.copy(alpha = 0.2f),
                                                RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                selectedLocationFilter = filter
                                                selectedSubCategory = null
                                                if (filter == "Minha localização") {
                                                    val fine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                    val coarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                    if (fine || coarse) {
                                                        requestAutomaticGPSLocation(context) { resolvedLoc ->
                                                            myCurrentGPSByWaze = resolvedLoc
                                                        }
                                                    } else {
                                                        showGPSTriggerPopup = true
                                                    }
                                                }
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = filter,
                                            color = if (isSelected) PrimaryGold else textVariantColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Categorias: Motel, Parques, Bar/Lanches, Restaurante, Shoppings
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CategoryGridCard(
                                    label = "Motel",
                                    icon = "🏩",
                                    isSelected = selectedCategory == "Motel",
                                    onClick = { selectedCategory = "Motel"; selectedSubCategory = null },
                                    modifier = Modifier.weight(1f),
                                    viewModel = viewModel
                                )
                                CategoryGridCard(
                                    label = "Parques",
                                    icon = "🌳",
                                    isSelected = selectedCategory == "Parques",
                                    onClick = { selectedCategory = "Parques"; selectedSubCategory = null },
                                    modifier = Modifier.weight(1f),
                                    viewModel = viewModel
                                )
                                CategoryGridCard(
                                    label = "Shoppings",
                                    icon = "🛍️",
                                    isSelected = selectedCategory == "Shoppings",
                                    onClick = { selectedCategory = "Shoppings"; selectedSubCategory = null },
                                    modifier = Modifier.weight(1f),
                                    viewModel = viewModel
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CategoryGridCard(
                                    label = "Bar/Lanches",
                                    icon = "🍔",
                                    isSelected = selectedCategory == "Bar/Lanches",
                                    onClick = { selectedCategory = "Bar/Lanches"; selectedSubCategory = null },
                                    modifier = Modifier.weight(1f),
                                    viewModel = viewModel
                                )
                                CategoryGridCard(
                                    label = "Restaurante",
                                    icon = "🍽️",
                                    isSelected = selectedCategory == "Restaurante",
                                    onClick = { selectedCategory = "Restaurante"; selectedSubCategory = null },
                                    modifier = Modifier.weight(1f),
                                    viewModel = viewModel
                                )
                            }

                            // Subcategories dropdown lists
                            val subCategories = when (selectedCategory) {
                                "Motel" -> listOf(
                                    Pair("💵 Barato / Econômico", "💵"),
                                    Pair("💎 Ostentação / Luxo", "💎"),
                                    Pair("🛁 Com Hidro / Ofurô", "🛁"),
                                    Pair("🌹 Suíte Temática", "🌹")
                                )
                                "Parques" -> listOf(
                                    Pair("🌳 Bosque & Natureza", "🌳"),
                                    Pair("🏃 Pista & Orla", "🏃"),
                                    Pair("🌊 Lago & Represa", "🌊"),
                                    Pair("🌺 Jardim Botânico", "🌺")
                                )
                                "Bar/Lanches" -> listOf(
                                    Pair("🍔 Hamburgueria", "🍔"),
                                    Pair("🍺 Bar & Boteco", "🍺"),
                                    Pair("🍸 Drinks & Coquetelaria", "🍸"),
                                    Pair("🍣 Sushi & Petiscos", "🍣"),
                                    Pair("☕ Cafeteria & Doceria", "☕")
                                )
                                "Restaurante" -> listOf(
                                    Pair("🕯️ Romântico a Dois", "🕯️"),
                                    Pair("🍝 Italiano & Massas", "🍝"),
                                    Pair("🍱 Japonês Premium", "🍱"),
                                    Pair("🥩 Churrascaria & Carnes", "🥩"),
                                    Pair("🥂 Alta Gastronomia", "🥂")
                                )
                                else -> listOf( // "Shoppings"
                                    Pair("🛍️ Shopping Center", "🛍️"),
                                    Pair("🎬 Cinema", "🎬"),
                                    Pair("🍨 Sorveteria & Gelato", "🍨"),
                                    Pair("🎳 Boliche & Jogos", "🎳")
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                subCategories.forEach { (subName, emoji) ->
                                    val isExpanded = selectedSubCategory == subName
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                if (isExpanded) PrimaryGold.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedSubCategory = if (isExpanded) null else subName
                                            },
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = subName,
                                                    color = textColor,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Button(
                                                        onClick = {
                                                            val query = "$subName próximo a $locationAddress"
                                                            try {
                                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"))
                                                                context.startActivity(webIntent)
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = textColor),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.height(24.dp)
                                                    ) {
                                                        Text("🗺️", fontSize = 10.sp)
                                                    }
                                                    Button(
                                                        onClick = {
                                                            val query = "$subName próximo a $locationAddress"
                                                            try {
                                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("waze://?q=${Uri.encode(query)}&navigate=yes"))
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=${Uri.encode(query)}&navigate=yes"))
                                                                context.startActivity(fallbackIntent)
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = textColor),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.height(24.dp)
                                                    ) {
                                                        Text("📍 Waze", fontSize = 8.sp)
                                                    }
                                                }
                                            }

                                            if (isExpanded) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                
                                                val venuesList = getVenueList(selectedCategory, subName, selectedLocationFilter, partner, locationAddress)
                                                if (venuesList.isEmpty()) {
                                                    Text("Nenhum local pré-mapeado para esta área.", color = textVariantColor, fontSize = 11.sp)
                                                } else {
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        venuesList.forEach { venue ->
                                                            Card(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.12f)),
                                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                                                            ) {
                                                                Column(modifier = Modifier.padding(8.dp)) {
                                                                    Row(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Column(modifier = Modifier.weight(1f)) {
                                                                            Text(text = venue.name, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                            Text(text = venue.address, color = textVariantColor.copy(alpha = 0.6f), fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                                        }
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Column(horizontalAlignment = Alignment.End) {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .clip(RoundedCornerShape(3.dp))
                                                                                    .background(PrimaryGold.copy(alpha = 0.1f))
                                                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                                                            ) {
                                                                                Text(text = venue.rating, color = PrimaryGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                                            }
                                                                            Text(text = venue.price, color = textVariantColor, fontSize = 8.sp)
                                                                            Spacer(modifier = Modifier.height(2.dp))
                                                                            Text(text = "🛣️ ${venue.distance}", color = Color(0xFF4CAF50), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                                        }
                                                                    }
                                                                    Spacer(modifier = Modifier.height(6.dp))
                                                                    Row(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                    ) {
                                                                        Button(
                                                                            onClick = {
                                                                                val query = "${venue.name}, ${venue.address}"
                                                                                val encoded = Uri.encode(query)
                                                                                try {
                                                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")))
                                                                                } catch (e: Exception) {
                                                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")))
                                                                                }
                                                                            },
                                                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                                                                            shape = RoundedCornerShape(4.dp),
                                                                            modifier = Modifier.weight(1f).height(26.dp),
                                                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                                        ) {
                                                                            Text("🗺️ Maps", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                                        }
                                                                        Button(
                                                                            onClick = {
                                                                                val query = "${venue.name}, ${venue.address}"
                                                                                val encoded = Uri.encode(query)
                                                                                try {
                                                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("waze://?q=$encoded&navigate=yes")))
                                                                                } catch (e: Exception) {
                                                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=$encoded&navigate=yes")))
                                                                                }
                                                                            },
                                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C1FF), contentColor = Color.White),
                                                                            shape = RoundedCornerShape(4.dp),
                                                                            modifier = Modifier.weight(1f).height(26.dp),
                                                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                                        ) {
                                                                            Text("🚗 Waze", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } // closes Column
                } // closes Box
            }
        }
    }

    if (showGPSTriggerPopup) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showGPSTriggerPopup = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White,
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📡 LOCALIZAÇÃO AUTOMÁTICA",
                        color = PrimaryGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "O aplicativo irá utilizar o GPS do seu celular de forma automática para rastrear sua latitude e longitude, localizando os melhores estabelecimentos e motéis próximos do seu raio de cobertura de forma totalmente automatizada.\n\nDeseja conceder a permissão agora?",
                        color = textColor,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showGPSTriggerPopup = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Manualmente", color = Color.Gray, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                showGPSTriggerPopup = false
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = OnPrimaryGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Autorizar GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreCardItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    viewModel: AgendaViewModel
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (viewModel.isDarkTheme) Level1DarkSurface else Color.White),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = if (viewModel.isDarkTheme) OnSurfaceGoldVariant.copy(alpha = 0.7f) else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = if (viewModel.isDarkTheme) OnSurfaceGold else Color.DarkGray, fontSize = 9.sp)
        }
    }
}

@Composable
fun CriteriaValueItem(
    label: String,
    value: String,
    icon: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "$label:", color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(text = value, color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
