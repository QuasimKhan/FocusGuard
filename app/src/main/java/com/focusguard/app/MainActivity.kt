package com.focusguard.app

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppEntry(
    val label: String,
    val packageName: String
)

private val CanvasBg = Color(0xFF090D16)
private val PanelBg = Color(0xFF101622)
private val ElevatedBg = Color(0xFF161F30)
private val HairlineBorder = Color(0xFF1E293B)
private val AccentCyan = Color(0xFF38BDF8)
private val AccentEmerald = Color(0xFF10B981)
private val AccentAmber = Color(0xFFF59E0B)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)

private val FocusGuardDarkScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color(0xFF062033),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = AccentEmerald,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    background = CanvasBg,
    onBackground = TextPrimary,
    surface = CanvasBg,
    onSurface = TextPrimary,
    surfaceVariant = PanelBg,
    onSurfaceVariant = TextMuted,
    outline = HairlineBorder,
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA)
)

class MainActivity : ComponentActivity() {
    private var serviceEnabledState = mutableStateOf(false)
    private var deviceAdminEnabledState = mutableStateOf(false)
    private var lastBlockedPkgState = mutableStateOf<String?>(null)
    private var frictionPkgState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            MaterialTheme(colorScheme = FocusGuardDarkScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CanvasBg
                ) {
                    FocusDashboard(
                        isServiceEnabled = serviceEnabledState.value,
                        isDeviceAdminEnabled = deviceAdminEnabledState.value,
                        blockedBannerPkg = lastBlockedPkgState.value,
                        frictionPkg = frictionPkgState.value,
                        onDismissBanner = { lastBlockedPkgState.value = null },
                        onDismissFriction = { frictionPkgState.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val blocked = intent.getStringExtra("blocked_pkg")
        val friction = intent.getStringExtra("friction_pkg")
        if (blocked != null) lastBlockedPkgState.value = blocked
        if (friction != null) frictionPkgState.value = friction
    }

    override fun onResume() {
        super.onResume()
        serviceEnabledState.value = FocusAccessibilityService.isConnected
        deviceAdminEnabledState.value = isDeviceAdminActive(this)
        if (FocusPrefs.isVpnEnabled(this) && VpnService.prepare(this) == null) {
            startForegroundService(Intent(this, DnsVpnService::class.java))
        }
    }

    private fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, FocusDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }
}

@Composable
fun FocusDashboard(
    isServiceEnabled: Boolean,
    isDeviceAdminEnabled: Boolean,
    blockedBannerPkg: String?,
    frictionPkg: String?,
    onDismissBanner: () -> Unit,
    onDismissFriction: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var masterActive by remember { mutableStateOf(FocusPrefs.isEnforcementActive(context)) }
    var blockShorts by remember { mutableStateOf(FocusPrefs.isShortsBlocked(context)) }
    var vpnActive by remember { mutableStateOf(FocusPrefs.isVpnEnabled(context)) }
    var staticDomainCount by remember { mutableIntStateOf(0) }

    var lockState by remember { mutableIntStateOf(FocusPrefs.getLockState(context)) }
    var cooldownRemainingMs by remember { mutableLongStateOf(FocusPrefs.getRemainingCooldownMs(context)) }
    var pomoRemainingMs by remember { mutableLongStateOf(FocusPrefs.getPomodoroRemainingMs(context)) }
    var isProtectionLocked by remember { mutableStateOf(FocusPrefs.isProtectionLocked(context)) }

    var availableUpdate by remember { mutableStateOf<ReleaseUpdate?>(null) }

    // Immediate visual feedback whenever the Master Toggle is tapped
    var toggleFeedbackText by remember { mutableStateOf<String?>(null) }
    var toggleFeedbackIsPositive by remember { mutableStateOf(true) }
    var toggleFeedbackTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(toggleFeedbackTick) {
        if (toggleFeedbackTick > 0) {
            delay(3200L)
            toggleFeedbackText = null
        }
    }

    // Silent 24h background GitHub Release check
    LaunchedEffect(Unit) {
        staticDomainCount = withContext(Dispatchers.IO) {
            DomainBlocklist.ensureLoaded(context)
        }
        val res = UpdateChecker.checkForUpdate(context, force = false)
        if (res is UpdateCheckResult.UpdateAvailable) {
            availableUpdate = res.update
        }
    }

    // 1-second global ticker for Cooldown, Pomodoro, and Study Window transitions
    LaunchedEffect(Unit) {
        while (true) {
            cooldownRemainingMs = FocusPrefs.getRemainingCooldownMs(context)
            lockState = FocusPrefs.getLockState(context)
            pomoRemainingMs = FocusPrefs.getPomodoroRemainingMs(context)
            isProtectionLocked = FocusPrefs.isProtectionLocked(context)
            masterActive = FocusPrefs.isEnforcementActive(context)
            delay(1000L)
        }
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            context.startForegroundService(Intent(context, DnsVpnService::class.java))
            vpnActive = true
        }
    }

    fun handleMasterToggle(requestedActive: Boolean) {
        if (!requestedActive && isProtectionLocked) {
            toggleFeedbackText = "Locked: Cannot pause shields during an active focus lock."
            toggleFeedbackIsPositive = false
            toggleFeedbackTick++
            return
        }
        masterActive = requestedActive
        FocusPrefs.setMasterActive(context, requestedActive)
        if (requestedActive) {
            toggleFeedbackText = "Shields Running — App blocks, Shorts filter & DNS guard are active."
            toggleFeedbackIsPositive = true
        } else {
            toggleFeedbackText = "Shields Paused — App, Shorts & web blocks are temporarily off."
            toggleFeedbackIsPositive = false
        }
        toggleFeedbackTick++
    }

    fun toggleVpn(enable: Boolean) {
        if (!enable && isProtectionLocked) return
        if (enable) {
            val prepIntent = VpnService.prepare(context)
            if (prepIntent != null) {
                vpnPermissionLauncher.launch(prepIntent)
            } else {
                context.startForegroundService(Intent(context, DnsVpnService::class.java))
                vpnActive = true
            }
        } else {
            val stopIntent = Intent(context, DnsVpnService::class.java).apply {
                action = DnsVpnService.ACTION_STOP
            }
            context.startService(stopIntent)
            vpnActive = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Brand & Master Control Header (Clean typographic layout without Q box)
        val statusColor = when {
            !masterActive -> AccentAmber
            isProtectionLocked -> AccentCyan
            else -> AccentEmerald
        }
        val statusBadgeText = when {
            lockState == FocusPrefs.LOCK_COOLING_DOWN -> "COOLDOWN · ${formatDuration(cooldownRemainingMs)}"
            pomoRemainingMs > 0L -> "DEEP WORK · ${formatDuration(pomoRemainingMs)}"
            FocusPrefs.isStudyWindowCurrentlyActive(context) -> "STUDY SCHEDULE · LOCKED"
            lockState == FocusPrefs.LOCK_ENGAGED -> "STRICT LOCK · ENGAGED"
            masterActive -> "SHIELDS RUNNING · PROTECTED"
            else -> "SHIELDS PAUSED · UNPROTECTED"
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = PanelBg,
            border = BorderStroke(
                width = 0.75.dp,
                color = if (masterActive) Color(0xFF1E3A5F) else HairlineBorder
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FocusGuard",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ElevatedBg,
                                border = BorderStroke(0.5.dp, Color(0xFF334155))
                            ) {
                                Text(
                                    text = "by Quasim.™",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.3.sp,
                                    color = AccentCyan,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(7.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(statusColor.copy(alpha = 0.13f))
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusBadgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp,
                                style = TextStyle(fontFeatureSettings = "tnum"),
                                color = statusColor
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { handleMasterToggle(!masterActive) }
                    ) {
                        Switch(
                            checked = masterActive,
                            onCheckedChange = { handleMasterToggle(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF042F2E),
                                checkedTrackColor = AccentEmerald,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = ElevatedBg
                            )
                        )
                        Text(
                            text = when {
                                isProtectionLocked -> "LOCKED"
                                masterActive -> "ACTIVE"
                                else -> "PAUSED"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = statusColor
                        )
                    }
                }

                AnimatedVisibility(visible = toggleFeedbackText != null) {
                    val bannerColor = if (toggleFeedbackIsPositive) AccentEmerald else AccentAmber
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = bannerColor.copy(alpha = 0.13f),
                        border = BorderStroke(0.5.dp, bannerColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = toggleFeedbackText ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (toggleFeedbackIsPositive) Color(0xFFA7F3D0) else Color(0xFFFDE68A),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // In-App GitHub Release Update Banner
        val update = availableUpdate
        if (update != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0C4A6E).copy(alpha = 0.55f),
                border = BorderStroke(0.75.dp, AccentCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update Available · ${update.latestTag}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = update.releaseNotes,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFFBAE6FD)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { uriHandler.openUri(update.downloadUrl) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { availableUpdate = null },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Later", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Mindful 15-Second Friction Overlay Card
        if (frictionPkg != null) {
            MindfulPauseCard(
                targetPackage = frictionPkg,
                onCancel = onDismissFriction,
                onProceed = {
                    FocusPrefs.grantTemporaryPass(frictionPkg)
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(frictionPkg)
                    onDismissFriction()
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Hard Block Intervention Banner
        if (blockedBannerPkg != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Blocked: $blockedBannerPkg",
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = onDismissBanner) { Text("OK") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Accessibility Service Warning
        if (!isServiceEnabled) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Accessibility Service Offline",
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }) {
                        Text("Enable")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Custom Segmented Pill Navigation
        SegmentedNavBar(
            items = listOf("Apps", "Web", "Study OS", "Armor"),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> AppsAndShortsTab(
                blockShorts = blockShorts,
                isLocked = isProtectionLocked,
                onToggleShorts = {
                    if (!it && isProtectionLocked) return@AppsAndShortsTab
                    blockShorts = it
                    FocusPrefs.setShortsBlocked(context, it)
                }
            )
            1 -> WebsitesAndDnsTab(
                vpnActive = vpnActive,
                isLocked = isProtectionLocked,
                staticDomainCount = staticDomainCount,
                onToggleVpn = { toggleVpn(it) }
            )
            2 -> StudyOsTab(
                isLocked = isProtectionLocked,
                pomoRemainingMs = pomoRemainingMs
            )
            3 -> ShieldLockTab(
                isServiceEnabled = isServiceEnabled,
                isDeviceAdminEnabled = isDeviceAdminEnabled,
                vpnActive = vpnActive,
                lockState = lockState,
                remainingMs = cooldownRemainingMs,
                onUpdateFound = { availableUpdate = it },
                onLockStateChanged = {
                    lockState = FocusPrefs.getLockState(context)
                    cooldownRemainingMs = FocusPrefs.getRemainingCooldownMs(context)
                    isProtectionLocked = FocusPrefs.isProtectionLocked(context)
                    masterActive = FocusPrefs.isEnforcementActive(context)
                }
            )
        }
    }
}

@Composable
fun SegmentedNavBar(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelBg)
            .border(0.75.dp, HairlineBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) ElevatedBg else Color.Transparent)
                    .border(
                        width = if (selected) 0.5.dp else 0.dp,
                        color = if (selected) Color(0xFF334155) else Color.Transparent,
                        shape = RoundedCornerShape(9.dp)
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) AccentCyan else TextMuted
                )
            }
        }
    }
}

@Composable
fun SectionPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = PanelBg,
        border = BorderStroke(0.75.dp, HairlineBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

@Composable
fun MindfulPauseCard(
    targetPackage: String,
    onCancel: () -> Unit,
    onProceed: () -> Unit
) {
    var secondsLeft by remember(targetPackage) { mutableIntStateOf(15) }

    LaunchedEffect(targetPackage) {
        secondsLeft = 15
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Mindful Breathing Pause",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Opening $targetPackage. Is this intentional or an impulse?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back to Study")
                }
                OutlinedButton(
                    onClick = onProceed,
                    enabled = secondsLeft == 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (secondsLeft > 0) "Wait ${secondsLeft}s" else "Open (5m Pass)",
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}

@Composable
fun AppsAndShortsTab(
    blockShorts: Boolean,
    isLocked: Boolean,
    onToggleShorts: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var blockedSet by remember { mutableStateOf(FocusPrefs.getBlockedPackages(context)) }
    var pausedSet by remember { mutableStateOf(FocusPrefs.getPausedPackages(context)) }
    var searchQuery by remember { mutableStateOf("") }

    val installedApps = remember {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                AppEntry(info.loadLabel(pm).toString(), pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kill YouTube Shorts & IG Reels", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Keeps regular videos & DMs open", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Switch(
                    checked = blockShorts,
                    enabled = !isLocked || !blockShorts,
                    onCheckedChange = onToggleShorts
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search apps (${blockedSet.size} blocked, ${pausedSet.size} paused)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredApps, key = { it.packageName }) { app ->
                val currentMode = when {
                    blockedSet.contains(app.packageName) -> FocusPrefs.APP_MODE_BLOCKED
                    pausedSet.contains(app.packageName) -> FocusPrefs.APP_MODE_PAUSE
                    else -> FocusPrefs.APP_MODE_OPEN
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.label,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val modes = listOf(
                            FocusPrefs.APP_MODE_OPEN to "Open",
                            FocusPrefs.APP_MODE_PAUSE to "15s",
                            FocusPrefs.APP_MODE_BLOCKED to "Block"
                        )
                        modes.forEach { (mode, label) ->
                            val selected = currentMode == mode
                            val canSelect = !isLocked || mode >= currentMode
                            val chipBg = when {
                                selected && mode == FocusPrefs.APP_MODE_BLOCKED -> Color(0xFF450A0A)
                                selected && mode == FocusPrefs.APP_MODE_PAUSE -> Color(0xFF451A03)
                                selected -> ElevatedBg
                                else -> Color.Transparent
                            }
                            val chipText = when {
                                selected && mode == FocusPrefs.APP_MODE_BLOCKED -> Color(0xFFFCA5A5)
                                selected && mode == FocusPrefs.APP_MODE_PAUSE -> Color(0xFFFCD34D)
                                selected -> AccentCyan
                                canSelect -> TextMuted
                                else -> Color(0xFF475569)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(chipBg)
                                    .border(0.5.dp, if (selected) chipText.copy(alpha = 0.5f) else HairlineBorder, RoundedCornerShape(8.dp))
                                    .clickable(enabled = canSelect) {
                                        FocusPrefs.setAppMode(context, app.packageName, mode)
                                        blockedSet = FocusPrefs.getBlockedPackages(context)
                                        pausedSet = FocusPrefs.getPausedPackages(context)
                                    }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = chipText)
                            }
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            }
        }
    }
}

@Composable
fun WebsitesAndDnsTab(
    vpnActive: Boolean,
    isLocked: Boolean,
    staticDomainCount: Int,
    onToggleVpn: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var domainInput by remember { mutableStateOf("") }
    var customDomains by remember { mutableStateOf(FocusPrefs.getCustomBlockedDomains(context).sorted()) }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Block Adult Contents", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "$staticDomainCount adult domains + mirror engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = vpnActive,
                    enabled = !isLocked || !vpnActive,
                    onCheckedChange = onToggleVpn
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open System VPN Settings", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = domainInput,
                onValueChange = { domainInput = it },
                label = { Text("Block domain or *wildcard*") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    FocusPrefs.addBlockedDomain(context, domainInput)
                    customDomains = FocusPrefs.getCustomBlockedDomains(context).sorted()
                    domainInput = ""
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Block")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(customDomains, key = { it }) { domain ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(domain, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (!isLocked) {
                        TextButton(onClick = {
                            FocusPrefs.removeBlockedDomain(context, domain)
                            customDomains = FocusPrefs.getCustomBlockedDomains(context).sorted()
                        }) {
                            Text("Remove", fontSize = 12.sp)
                        }
                    } else {
                        Text("Locked", fontSize = 11.sp, color = TextMuted)
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            }
        }
    }
}

@Composable
fun StudyOsTab(
    isLocked: Boolean,
    pomoRemainingMs: Long
) {
    val context = LocalContext.current
    var scheduleEnabled by remember { mutableStateOf(FocusPrefs.isScheduleEnabled(context)) }
    var startMin by remember { mutableIntStateOf(FocusPrefs.getScheduleStartMinutes(context)) }
    var endMin by remember { mutableIntStateOf(FocusPrefs.getScheduleEndMinutes(context)) }

    var selectedPomoMins by remember { mutableIntStateOf(50) }
    var lockDuringPomo by remember { mutableStateOf(FocusPrefs.isPomodoroLockEnabled(context)) }
    val todayMins = FocusPrefs.getTodayFocusedMinutes(context)
    val todaySessions = FocusPrefs.getTodaySessions(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SectionPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Deep-Work Study Timer", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    text = "Today: ${todayMins}m ($todaySessions)",
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (pomoRemainingMs > 0L) {
                Text(
                    text = formatDuration(pomoRemainingMs),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    color = AccentCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!lockDuringPomo) {
                    OutlinedButton(
                        onClick = { FocusPrefs.stopPomodoro(context) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stop Timer")
                    }
                } else {
                    Text(
                        "Distractions & Settings locked until session completes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(25, 50, 90).forEach { mins ->
                        val active = selectedPomoMins == mins
                        if (active) {
                            Button(
                                onClick = { selectedPomoMins = mins },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("${mins}m") }
                        } else {
                            OutlinedButton(
                                onClick = { selectedPomoMins = mins },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("${mins}m") }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Lock distractions during session",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Switch(
                        checked = lockDuringPomo,
                        onCheckedChange = {
                            lockDuringPomo = it
                            FocusPrefs.setPomodoroLockEnabled(context, it)
                        }
                    )
                }

                Button(
                    onClick = { FocusPrefs.startPomodoro(context, selectedPomoMins) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start ${selectedPomoMins}-Minute Study Block", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SectionPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily Auto-Lock Window", fontWeight = FontWeight.Bold)
                    Text(
                        "Automatically enforces blocks & settings during study hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = scheduleEnabled,
                    enabled = !isLocked || !scheduleEnabled,
                    onCheckedChange = {
                        if (!it && isLocked) return@Switch
                        scheduleEnabled = it
                        FocusPrefs.setScheduleEnabled(context, it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TimeAdjustRow(
                label = "Start Time",
                minutesFromMidnight = startMin,
                enabled = !isLocked,
                onAdjust = { delta ->
                    FocusPrefs.adjustScheduleMinutes(context, isStart = true, deltaMinutes = delta)
                    startMin = FocusPrefs.getScheduleStartMinutes(context)
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            TimeAdjustRow(
                label = "End Time",
                minutesFromMidnight = endMin,
                enabled = !isLocked,
                onAdjust = { delta ->
                    FocusPrefs.adjustScheduleMinutes(context, isStart = false, deltaMinutes = delta)
                    endMin = FocusPrefs.getScheduleEndMinutes(context)
                }
            )
        }
    }
}

@Composable
fun TimeAdjustRow(
    label: String,
    minutesFromMidnight: Int,
    enabled: Boolean,
    onAdjust: (Int) -> Unit
) {
    val hours = minutesFromMidnight / 60
    val mins = minutesFromMidnight % 60
    val formatted = String.format("%02d:%02d", hours, mins)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: $formatted",
            fontWeight = FontWeight.Medium,
            style = TextStyle(fontFeatureSettings = "tnum"),
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            onClick = { onAdjust(-30) },
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) { Text("-30m", fontSize = 12.sp) }
        Spacer(modifier = Modifier.width(6.dp))
        OutlinedButton(
            onClick = { onAdjust(30) },
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) { Text("+30m", fontSize = 12.sp) }
    }
}

@Composable
fun ShieldLockTab(
    isServiceEnabled: Boolean,
    isDeviceAdminEnabled: Boolean,
    vpnActive: Boolean,
    lockState: Int,
    remainingMs: Long,
    onUpdateFound: (ReleaseUpdate) -> Unit,
    onLockStateChanged: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val installedVersion = remember { UpdateChecker.getInstalledVersion(context) }

    var updateStatusText by remember { mutableStateOf<String?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    val presets = listOf(
        15 to "15m",
        60 to "1h",
        360 to "6h",
        1440 to "24h",
        10080 to "7d"
    )
    var selectedMinutes by remember { mutableIntStateOf(1440) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SectionPanel {
            Text("System Armor Status", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            StatusLine("Accessibility & Settings Watchdog", isServiceEnabled)
            StatusLine("Local DNS & Mirror Firewall", vpnActive)
            StatusLine("Device Admin (Blocks Uninstall)", isDeviceAdminEnabled)

            if (!isDeviceAdminEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val adminComponent = ComponentName(context, FocusDeviceAdminReceiver::class.java)
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                            putExtra(
                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "Prevents uninstalling FocusGuard during study and strict-lock periods."
                            )
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Activate Device Admin Uninstall Lock")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SectionPanel {
            Text("Anti-Impulse Strict Lock", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            when (lockState) {
                FocusPrefs.LOCK_UNLOCKED -> {
                    Text(
                        "Locks active blocks and blocks Android Settings for FocusGuard, Private DNS, and Date/Time. Wait out the chosen cooldown to unlock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (mins, label) ->
                            val isSelected = selectedMinutes == mins
                            if (isSelected) {
                                Button(
                                    onClick = { selectedMinutes = mins },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) { Text(label, fontSize = 12.sp) }
                            } else {
                                OutlinedButton(
                                    onClick = { selectedMinutes = mins },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) { Text(label, fontSize = 12.sp) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            FocusPrefs.engageStrictLock(context, selectedMinutes)
                            onLockStateChanged()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Engage Strict Lock", fontWeight = FontWeight.Bold)
                    }
                }

                FocusPrefs.LOCK_ENGAGED -> {
                    val configuredMins = FocusPrefs.getConfiguredCooldownMinutes(context)
                    Text(
                        "Strict Lock is ACTIVE. You can still tighten app or website blocks, but nothing can be disabled until a ${configuredMins}-minute cooldown finishes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            FocusPrefs.requestUnlock(context)
                            onLockStateChanged()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Unlock Cooldown (${configuredMins}m)")
                    }
                }

                FocusPrefs.LOCK_COOLING_DOWN -> {
                    Text(
                        "Cooldown in progress. Strict Lock remains enforced until timer hits 00:00:00:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatDuration(remainingMs),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        color = AccentCyan
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            FocusPrefs.cancelUnlockRequest(context)
                            onLockStateChanged()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Unlock & Re-Lock Immediately")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Developer, Updates & Contact Panel
        SectionPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Developer & Releases", fontWeight = FontWeight.Bold)
                    Text(
                        text = "FocusGuard v$installedVersion",
                        fontSize = 12.sp,
                        color = AccentCyan
                    )
                    Text(
                        text = "Developed by Quasim.™",
                        fontSize = 12.sp,
                        color = AccentCyan
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (isCheckingUpdate) return@OutlinedButton
                        isCheckingUpdate = true
                        updateStatusText = "Checking GitHub..."
                        scope.launch {
                            when (val res = UpdateChecker.checkForUpdate(context, force = true)) {
                                is UpdateCheckResult.UpdateAvailable -> {
                                    updateStatusText = "New release ${res.update.latestTag} available!"
                                    onUpdateFound(res.update)
                                }
                                is UpdateCheckResult.UpToDate -> {
                                    updateStatusText = "Up to date (v${res.currentVersion})"
                                }
                                is UpdateCheckResult.Error -> {
                                    updateStatusText = res.reason
                                }
                            }
                            isCheckingUpdate = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(if (isCheckingUpdate) "Checking..." else "Check Updates", fontSize = 11.sp)
                }
            }

            if (updateStatusText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = updateStatusText ?: "",
                    fontSize = 11.sp,
                    color = AccentEmerald
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "100% On-Device Native Kotlin · Zero Cloud Tracking",
                fontSize = 11.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ContactChip(
                    label = "GitHub",
                    modifier = Modifier.weight(1f),
                    onClick = { uriHandler.openUri("https://github.com/quasimkhan/FocusGuard") }
                )
                ContactChip(
                    label = "Email",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:quasimdotdev@gmail.com"))
                        try { context.startActivity(mail) } catch (_: Exception) {}
                    }
                )
                ContactChip(
                    label = "X",
                    modifier = Modifier.weight(1f),
                    onClick = { uriHandler.openUri("https://x.com/quasimdotdev") }
                )
                ContactChip(
                    label = "LinkedIn",
                    modifier = Modifier.weight(1f),
                    onClick = { uriHandler.openUri("https://linkedin.com/in/quasimkhan") }
                )
            }
        }
    }
}

@Composable
fun ContactChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedBg)
            .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
fun StatusLine(label: String, active: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (active) "ACTIVE" else "OFF",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) AccentEmerald else MaterialTheme.colorScheme.error
        )
    }
}

fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return String.format("%02d:%02d:%02d", hours, mins, secs)
}
