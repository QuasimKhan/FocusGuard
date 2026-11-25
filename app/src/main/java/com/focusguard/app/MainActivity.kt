package com.focusguard.app

import android.app.Activity
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias ConfirmationRequester = (String, String, String, () -> Unit) -> Unit

private data class ConfirmationPrompt(
        val title: String,
        val message: String,
        val confirmLabel: String,
        val pendingAction: PendingUserAction
)

private val CanvasBg: Color
        @Composable get() = MaterialTheme.colorScheme.background
private val PanelBg: Color
        @Composable get() = MaterialTheme.colorScheme.surface
private val ElevatedBg: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val HairlineBorder: Color
        @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val AccentCyan: Color
        @Composable get() = MaterialTheme.colorScheme.primary
private val AccentEmerald: Color
        @Composable get() = MaterialTheme.colorScheme.secondary
private val AccentAmber: Color
        @Composable get() = MaterialTheme.colorScheme.tertiary
private val TextPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
private val TextMuted: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val FocusGuardDarkScheme =
        darkColorScheme(
                primary = Color(0xFF62BCEB),
                onPrimary = Color(0xFF082433),
                primaryContainer = Color(0xFF173F54),
                onPrimaryContainer = Color(0xFFD8F2FF),
                secondary = Color(0xFF79C6A5),
                onSecondary = Color(0xFF0B2D24),
                secondaryContainer = Color(0xFF17493B),
                onSecondaryContainer = Color(0xFFCCF3E1),
                tertiary = Color(0xFFFFC66D),
                onTertiary = Color(0xFF3B2600),
                tertiaryContainer = Color(0xFF5C410E),
                onTertiaryContainer = Color(0xFFFFE8BC),
                background = Color(0xFF0B1118),
                onBackground = Color(0xFFE9EFF5),
                surface = Color(0xFF111B25),
                onSurface = Color(0xFFE9EFF5),
                surfaceVariant = Color(0xFF192633),
                onSurfaceVariant = Color(0xFFB3C0CC),
                outline = Color(0xFF536575),
                outlineVariant = Color(0xFF2C3B49),
                error = Color(0xFFFFB4AB),
                errorContainer = Color(0xFF5C2621),
                onErrorContainer = Color(0xFFFFE1DE)
        )

private val FocusGuardLightScheme =
        lightColorScheme(
                primary = Color(0xFF176A82),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD6EEF5),
                onPrimaryContainer = Color(0xFF123B49),
                secondary = Color(0xFF286A52),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFD9EFE5),
                onSecondaryContainer = Color(0xFF173D30),
                tertiary = Color(0xFF805900),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFF4E8C9),
                onTertiaryContainer = Color(0xFF4A3505),
                background = Color(0xFFF5F7F9),
                onBackground = Color(0xFF1B2630),
                surface = Color.White,
                onSurface = Color(0xFF1B2630),
                surfaceVariant = Color(0xFFEDF1F4),
                onSurfaceVariant = Color(0xFF52616E),
                outline = Color(0xFF788692),
                outlineVariant = Color(0xFFD4DCE2),
                error = Color(0xFFB3261E),
                errorContainer = Color(0xFFF9DEDC),
                onErrorContainer = Color(0xFF410E0B)
        )

private val FocusGuardTypography =
        Typography(
                headlineSmall =
                        TextStyle(
                                fontSize = 24.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.SemiBold
                        ),
                titleLarge =
                        TextStyle(
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.SemiBold
                        ),
                titleMedium =
                        TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.SemiBold
                        ),
                bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
                bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
                labelLarge =
                        TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium
                        ),
                labelMedium =
                        TextStyle(
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium
                        )
        )

class MainActivity : ComponentActivity() {
        private var serviceEnabledState = mutableStateOf(false)
        private var deviceAdminEnabledState = mutableStateOf(false)
        private var lastBlockedPkgState = mutableStateOf<String?>(null)
        private var frictionPkgState = mutableStateOf<String?>(null)

        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                handleIncomingIntent(intent)
                FocusNotifier.initializeChannels(this)

                setContent {
                        val colorScheme =
                                if (isSystemInDarkTheme()) FocusGuardDarkScheme
                                else FocusGuardLightScheme
                        MaterialTheme(
                                colorScheme = colorScheme,
                                typography = FocusGuardTypography
                        ) {
                                Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.background
                                ) {
                                        FocusDashboard(
                                                isServiceEnabled = serviceEnabledState.value,
                                                isDeviceAdminEnabled =
                                                        deviceAdminEnabledState.value,
                                                blockedBannerPkg = lastBlockedPkgState.value,
                                                frictionPkg = frictionPkgState.value,
                                                onDismissBanner = {
                                                        lastBlockedPkgState.value = null
                                                },
                                                onDismissFriction = {
                                                        frictionPkgState.value = null
                                                }
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
                val dpm =
                        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as
                                DevicePolicyManager
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
        var cooldownRemainingMs by remember {
                mutableLongStateOf(FocusPrefs.getRemainingCooldownMs(context))
        }
        var pomoRemainingMs by remember {
                mutableLongStateOf(FocusPrefs.getPomodoroRemainingMs(context))
        }
        var isProtectionLocked by remember {
                mutableStateOf(FocusPrefs.isProtectionLocked(context))
        }
        var appModesLocked by remember {
                mutableStateOf(
                        FocusPrefs.isStrictLockActive(context) ||
                                FocusPrefs.isPomodoroLockCurrentlyActive(context)
                )
        }

        var availableUpdate by remember { mutableStateOf<ReleaseUpdate?>(null) }
        var pendingConfirmation by remember { mutableStateOf<ConfirmationPrompt?>(null) }
        val requestConfirmation: ConfirmationRequester = { title, message, confirmLabel, action ->
                pendingConfirmation =
                        ConfirmationPrompt(
                                title = title,
                                message = message,
                                confirmLabel = confirmLabel,
                                pendingAction = PendingUserAction(action)
                        )
        }

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
                staticDomainCount =
                        withContext(Dispatchers.IO) { DomainBlocklist.ensureLoaded(context) }
                val res = UpdateChecker.checkForUpdate(context, force = false)
                if (res is UpdateCheckResult.UpdateAvailable) {
                        availableUpdate = res.update
                        FocusNotifier.sendUpdateNotification(context, res.update)
                }
        }

        // 1-second global ticker for Cooldown, Pomodoro, and Study Window transitions
        LaunchedEffect(Unit) {
                while (true) {
                        cooldownRemainingMs = FocusPrefs.getRemainingCooldownMs(context)
                        lockState = FocusPrefs.getLockState(context)
                        pomoRemainingMs = FocusPrefs.getPomodoroRemainingMs(context)
                        isProtectionLocked = FocusPrefs.isProtectionLocked(context)
                        appModesLocked =
                                FocusPrefs.isStrictLockActive(context) ||
                                        FocusPrefs.isPomodoroLockCurrentlyActive(context)
                        masterActive = FocusPrefs.isEnforcementActive(context)
                        vpnActive = FocusPrefs.isVpnEnabled(context)
                        delay(1000L)
                }
        }

        val vpnPermissionLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                        VpnConsentGate.onAuthorizationResult(
                                result.resultCode,
                                Activity.RESULT_OK
                        ) {
                                context.startForegroundService(
                                        Intent(context, DnsVpnService::class.java)
                                )
                        }
                }

        fun applyMasterToggle(requestedActive: Boolean) {
                if (!requestedActive && isProtectionLocked) {
                        toggleFeedbackText =
                                "Locked: Cannot pause shields during an active focus lock."
                        toggleFeedbackIsPositive = false
                        toggleFeedbackTick++
                        return
                }
                masterActive = requestedActive
                FocusPrefs.setMasterActive(context, requestedActive)
                if (requestedActive) {
                        toggleFeedbackText =
                                "Shields Running — App blocks, Shorts filter & DNS guard are active."
                        toggleFeedbackIsPositive = true
                } else {
                        toggleFeedbackText =
                                "Shields Paused — App, Shorts & web blocks are temporarily off."
                        toggleFeedbackIsPositive = false
                }
                toggleFeedbackTick++
        }

        fun handleMasterToggle(requestedActive: Boolean) {
                if (requestedActive == masterActive) return
                if (requestedActive) {
                        requestConfirmation(
                                "Turn on FocusGuard protection?",
                                "App rules, Shorts filtering, and enabled web protection will enforce their current settings.",
                                "Turn on"
                        ) { applyMasterToggle(true) }
                } else {
                        applyMasterToggle(false)
                }
        }

        fun toggleVpn(enable: Boolean) {
                if (!enable && isProtectionLocked) return
                if (enable) {
                        val prepIntent = VpnService.prepare(context)
                        if (prepIntent != null) {
                                vpnPermissionLauncher.launch(prepIntent)
                        } else {
                                context.startForegroundService(
                                        Intent(context, DnsVpnService::class.java)
                                )
                        }
                } else {
                        FocusPrefs.setVpnEnabled(context, false)
                        val stopIntent =
                                Intent(context, DnsVpnService::class.java).apply {
                                        action = DnsVpnService.ACTION_STOP
                                }
                        context.startService(stopIntent)
                        vpnActive = false
                }
        }

        BoxWithConstraints(
                modifier =
                        Modifier.fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .imePadding()
                                .padding(horizontal = 16.dp)
        ) {
                val compactDashboard =
                        maxHeight < 620.dp ||
                                androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(vertical = if (compactDashboard) 6.dp else 10.dp)
                ) {
                        // Brand & Master Control Header (Clean typographic layout without Q box)
                        val statusColor =
                                when {
                                        !masterActive -> AccentAmber
                                        isProtectionLocked -> AccentCyan
                                        else -> AccentEmerald
                                }
                        val statusBadgeText =
                                when {
                                        lockState == FocusPrefs.LOCK_COOLING_DOWN ->
                                                "COOLDOWN · ${formatDuration(cooldownRemainingMs)}"
                                        pomoRemainingMs > 0L ->
                                                "DEEP WORK · ${formatDuration(pomoRemainingMs)}"
                                        FocusPrefs.isStudyWindowCurrentlyActive(context) ->
                                                "STUDY SCHEDULE · LOCKED"
                                        lockState == FocusPrefs.LOCK_ENGAGED ->
                                                "STRICT LOCK · ENGAGED"
                                        masterActive -> "SHIELDS RUNNING · PROTECTED"
                                        else -> "SHIELDS PAUSED · UNPROTECTED"
                                }

                        Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = PanelBg,
                                border =
                                        BorderStroke(
                                                width = 0.75.dp,
                                                color =
                                                        if (masterActive) Color(0xFF1E3A5F)
                                                        else HairlineBorder
                                        )
                        ) {
                                Column(
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 14.dp,
                                                        vertical =
                                                                if (compactDashboard) 9.dp
                                                                else 14.dp
                                                )
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                FocusGuardMark(
                                                        size =
                                                                if (compactDashboard) 36.dp
                                                                else 40.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                                                        val compactHeader =
                                                                maxWidth < 220.dp ||
                                                                        androidx.compose.ui.platform
                                                                                .LocalDensity
                                                                                .current
                                                                                .fontScale > 1.2f
                                                        Column {
                                                                if (compactHeader) {
                                                                        Text(
                                                                                text = "FocusGuard",
                                                                                fontSize = 20.sp,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .ExtraBold,
                                                                                letterSpacing =
                                                                                        (-0.5).sp,
                                                                                color = TextPrimary
                                                                        )
                                                                        Text(
                                                                                "by ECN™",
                                                                                fontSize = 9.sp,
                                                                                color = AccentCyan
                                                                        )
                                                                } else {
                                                                        Row(
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically
                                                                        ) {
                                                                                Text(
                                                                                        text =
                                                                                                "FocusGuard",
                                                                                        fontSize =
                                                                                                22.sp,
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .ExtraBold,
                                                                                        letterSpacing =
                                                                                                (-0.5).sp,
                                                                                        color =
                                                                                                TextPrimary
                                                                                )
                                                                                Spacer(
                                                                                        modifier =
                                                                                                Modifier.width(
                                                                                                        8.dp
                                                                                                )
                                                                                )
                                                                                Surface(
                                                                                        shape =
                                                                                                RoundedCornerShape(
                                                                                                        6.dp
                                                                                                ),
                                                                                        color =
                                                                                                ElevatedBg,
                                                                                        border =
                                                                                                BorderStroke(
                                                                                                        0.5.dp,
                                                                                                        Color(
                                                                                                                0xFF334155
                                                                                                        )
                                                                                                )
                                                                                ) {
                                                                                        Text(
                                                                                                text =
                                                                                                        "by ECN™",
                                                                                                fontSize =
                                                                                                        10.sp,
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .SemiBold,
                                                                                                letterSpacing =
                                                                                                        0.3.sp,
                                                                                                color =
                                                                                                        AccentCyan,
                                                                                                modifier =
                                                                                                        Modifier.padding(
                                                                                                                horizontal =
                                                                                                                        7.dp,
                                                                                                                vertical =
                                                                                                                        2.dp
                                                                                                        )
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        if (compactDashboard
                                                                                        )
                                                                                                4.dp
                                                                                        else 7.dp
                                                                                )
                                                                )
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .clip(
                                                                                                RoundedCornerShape(
                                                                                                        50
                                                                                                )
                                                                                        )
                                                                                        .background(
                                                                                                statusColor
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.13f
                                                                                                        )
                                                                                        )
                                                                                        .padding(
                                                                                                horizontal =
                                                                                                        8.dp,
                                                                                                vertical =
                                                                                                        3.dp
                                                                                        ),
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                        7.dp
                                                                                                )
                                                                                                .clip(
                                                                                                        CircleShape
                                                                                                )
                                                                                                .background(
                                                                                                        statusColor
                                                                                                )
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                6.dp
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                text =
                                                                                        statusBadgeText,
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        ),
                                                                                fontSize =
                                                                                        if (compactDashboard
                                                                                        )
                                                                                                9.sp
                                                                                        else 11.sp,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold,
                                                                                letterSpacing =
                                                                                        0.2.sp,
                                                                                style =
                                                                                        TextStyle(
                                                                                                fontFeatureSettings =
                                                                                                        "tnum"
                                                                                        ),
                                                                                color = statusColor,
                                                                                maxLines =
                                                                                        if (compactDashboard
                                                                                        )
                                                                                                2
                                                                                        else 1,
                                                                                overflow =
                                                                                        TextOverflow
                                                                                                .Ellipsis
                                                                        )
                                                                }
                                                        }
                                                }

                                                Column(
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally,
                                                        modifier =
                                                                Modifier.clickable {
                                                                        handleMasterToggle(
                                                                                !masterActive
                                                                        )
                                                                }
                                                ) {
                                                        Switch(
                                                                checked = masterActive,
                                                                onCheckedChange = {
                                                                        handleMasterToggle(it)
                                                                },
                                                                colors =
                                                                        SwitchDefaults.colors(
                                                                                checkedThumbColor =
                                                                                        Color(
                                                                                                0xFF042F2E
                                                                                        ),
                                                                                checkedTrackColor =
                                                                                        AccentEmerald,
                                                                                uncheckedThumbColor =
                                                                                        TextMuted,
                                                                                uncheckedTrackColor =
                                                                                        ElevatedBg
                                                                        )
                                                        )
                                                        Text(
                                                                text =
                                                                        when {
                                                                                isProtectionLocked ->
                                                                                        "LOCKED"
                                                                                masterActive ->
                                                                                        "ACTIVE"
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
                                                val bannerColor =
                                                        if (toggleFeedbackIsPositive) AccentEmerald
                                                        else AccentAmber
                                                Surface(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(top = 10.dp),
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = bannerColor.copy(alpha = 0.13f),
                                                        border =
                                                                BorderStroke(
                                                                        0.5.dp,
                                                                        bannerColor.copy(
                                                                                alpha = 0.4f
                                                                        )
                                                                )
                                                ) {
                                                        Text(
                                                                text = toggleFeedbackText ?: "",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color =
                                                                        if (toggleFeedbackIsPositive
                                                                        )
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSecondaryContainer
                                                                        else
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onTertiaryContainer,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                horizontal = 10.dp,
                                                                                vertical = 7.dp
                                                                        )
                                                        )
                                                }
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(if (compactDashboard) 6.dp else 10.dp))

                        // In-App GitHub Release Update Banner
                        val update = availableUpdate
                        if (update != null) {
                                Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF0C4A6E).copy(alpha = 0.55f),
                                        border =
                                                BorderStroke(0.75.dp, AccentCyan.copy(alpha = 0.5f))
                                ) {
                                        Row(
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 12.dp,
                                                                vertical = 10.dp
                                                        ),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                                text =
                                                                        "Update Available · ${update.latestTag}",
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
                                                        onClick = {
                                                                uriHandler.openUri(
                                                                        update.downloadUrl
                                                                )
                                                        },
                                                        contentPadding =
                                                                PaddingValues(
                                                                        horizontal = 12.dp,
                                                                        vertical = 6.dp
                                                                )
                                                ) {
                                                        Text(
                                                                "Download",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }
                                                TextButton(
                                                        onClick = { availableUpdate = null },
                                                        contentPadding =
                                                                PaddingValues(horizontal = 6.dp)
                                                ) {
                                                        Text(
                                                                "Later",
                                                                fontSize = 11.sp,
                                                                color = TextMuted
                                                        )
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
                                                val launchIntent =
                                                        context.packageManager
                                                                .getLaunchIntentForPackage(
                                                                        frictionPkg
                                                                )
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
                                        border =
                                                BorderStroke(
                                                        0.5.dp,
                                                        MaterialTheme.colorScheme.error.copy(
                                                                alpha = 0.4f
                                                        )
                                                ),
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Row(
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 12.dp,
                                                                vertical = 8.dp
                                                        ),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        text = "Blocked: $blockedBannerPkg",
                                                        modifier = Modifier.weight(1f),
                                                        fontSize = 13.sp,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onErrorContainer
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
                                        Column(
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 12.dp,
                                                                vertical = 9.dp
                                                        )
                                        ) {
                                                Text(
                                                        text = "Accessibility Service Offline",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onTertiaryContainer
                                                )
                                                Text(
                                                        text =
                                                                "App Pause/Block rules and the blocked-app screen need this service. Open Android Settings → Accessibility → FocusGuard, allow the service, then return here.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onTertiaryContainer,
                                                        modifier = Modifier.padding(top = 3.dp)
                                                )
                                                Button(
                                                        onClick = {
                                                                context.startActivity(
                                                                        Intent(
                                                                                Settings.ACTION_ACCESSIBILITY_SETTINGS
                                                                        )
                                                                )
                                                        },
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(top = 6.dp),
                                                        contentPadding =
                                                                PaddingValues(
                                                                        horizontal = 12.dp,
                                                                        vertical =
                                                                                if (compactDashboard
                                                                                )
                                                                                        5.dp
                                                                                else 8.dp
                                                                )
                                                ) { Text("Open Accessibility settings") }
                                        }
                                }
                                Spacer(
                                        modifier =
                                                Modifier.height(
                                                        if (compactDashboard) 5.dp else 8.dp
                                                )
                                )
                        }

                        // Custom Segmented Pill Navigation
                        SegmentedNavBar(
                                items = listOf("Apps", "Web", "Study OS", "Armor"),
                                selectedIndex = selectedTab,
                                onSelect = { selectedTab = it }
                        )

                        Spacer(modifier = Modifier.height(if (compactDashboard) 6.dp else 12.dp))

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                when (selectedTab) {
                                        0 ->
                                                AppsAndShortsTab(
                                                        blockShorts = blockShorts,
                                                        isLocked = isProtectionLocked,
                                                        appModesLocked = appModesLocked,
                                                        requestConfirmation = requestConfirmation,
                                                        onToggleShorts = {
                                                                if (!it && isProtectionLocked)
                                                                        return@AppsAndShortsTab
                                                                val apply = {
                                                                        blockShorts = it
                                                                        FocusPrefs.setShortsBlocked(
                                                                                context,
                                                                                it
                                                                        )
                                                                }
                                                                if (it && !blockShorts) {
                                                                        requestConfirmation(
                                                                                "Block Shorts and Reels?",
                                                                                "FocusGuard will exit YouTube Shorts and Instagram Reels while keeping other app content available.",
                                                                                "Enable filter",
                                                                                apply
                                                                        )
                                                                } else apply()
                                                        }
                                                )
                                        1 ->
                                                WebsitesAndDnsTab(
                                                        vpnActive = vpnActive,
                                                        isLocked = isProtectionLocked,
                                                        staticDomainCount = staticDomainCount,
                                                        onToggleVpn = {
                                                                if (it && !vpnActive) {
                                                                        requestConfirmation(
                                                                                "Enable the local DNS shield?",
                                                                                "FocusGuard will use Android's local VPN permission to filter DNS requests on this device.",
                                                                                "Continue"
                                                                        ) { toggleVpn(true) }
                                                                } else toggleVpn(it)
                                                        }
                                                )
                                        2 ->
                                                StudyOsTab(
                                                        isLocked = isProtectionLocked,
                                                        pomoRemainingMs = pomoRemainingMs,
                                                        requestConfirmation = requestConfirmation
                                                )
                                        3 ->
                                                ShieldLockTab(
                                                        isServiceEnabled = isServiceEnabled,
                                                        isDeviceAdminEnabled = isDeviceAdminEnabled,
                                                        vpnActive = vpnActive,
                                                        lockState = lockState,
                                                        remainingMs = cooldownRemainingMs,
                                                        requestConfirmation = requestConfirmation,
                                                        onUpdateFound = { availableUpdate = it },
                                                        onLockStateChanged = {
                                                                lockState =
                                                                        FocusPrefs.getLockState(
                                                                                context
                                                                        )
                                                                cooldownRemainingMs =
                                                                        FocusPrefs
                                                                                .getRemainingCooldownMs(
                                                                                        context
                                                                                )
                                                                isProtectionLocked =
                                                                        FocusPrefs
                                                                                .isProtectionLocked(
                                                                                        context
                                                                                )
                                                                masterActive =
                                                                        FocusPrefs
                                                                                .isEnforcementActive(
                                                                                        context
                                                                                )
                                                        }
                                                )
                                }
                        }

                        pendingConfirmation?.let { prompt ->
                                ConfirmationPromptDialog(
                                        prompt = prompt,
                                        onDismiss = {
                                                prompt.pendingAction.cancel()
                                                pendingConfirmation = null
                                        },
                                        onConfirm = {
                                                pendingConfirmation = null
                                                prompt.pendingAction.confirm()
                                        }
                                )
                        }
                }
        }
}

@Composable
private fun ConfirmationPromptDialog(
        prompt: ConfirmationPrompt,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
) {
        ResponsivePromptDialog(
                title = prompt.title,
                message = prompt.message,
                confirmLabel = prompt.confirmLabel,
                onDismiss = onDismiss,
                onConfirm = onConfirm
        )
}

@Composable
private fun ResponsivePromptDialog(
        title: String,
        message: String,
        confirmLabel: String,
        dismissLabel: String = "Cancel",
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
) {
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
                BoxWithConstraints(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(horizontal = 24.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                ) {
                        val stackActions = maxWidth < 360.dp || fontScale > 1.3f
                        Surface(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .widthIn(max = 480.dp)
                                                .heightIn(max = maxHeight),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 6.dp
                        ) {
                                Column(
                                        modifier =
                                                Modifier.padding(24.dp)
                                                        .heightIn(
                                                                max =
                                                                        (maxHeight - 48.dp)
                                                                                .coerceAtLeast(
                                                                                        120.dp
                                                                                )
                                                        )
                                                        .verticalScroll(rememberScrollState())
                                ) {
                                        Text(title, style = MaterialTheme.typography.headlineSmall)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(message, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        if (stackActions) {
                                                Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(4.dp)
                                                ) {
                                                        Button(
                                                                onClick = onConfirm,
                                                                modifier = Modifier.fillMaxWidth()
                                                        ) { Text(confirmLabel) }
                                                        TextButton(
                                                                onClick = onDismiss,
                                                                modifier = Modifier.fillMaxWidth()
                                                        ) { Text(dismissLabel) }
                                                }
                                        } else {
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        TextButton(onClick = onDismiss) {
                                                                Text(dismissLabel)
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Button(onClick = onConfirm) {
                                                                Text(confirmLabel)
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
fun SegmentedNavBar(items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        val density = androidx.compose.ui.platform.LocalDensity.current
        val scrollState = rememberScrollState()
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compactNavigation = maxWidth < 390.dp || fontScale > 1.15f
                LaunchedEffect(selectedIndex, compactNavigation) {
                        if (compactNavigation) {
                                with(density) {
                                        scrollState.animateScrollTo(
                                                (selectedIndex * 92).dp.roundToPx()
                                        )
                                }
                        }
                }
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PanelBg)
                                        .border(0.75.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                        .padding(4.dp)
                                        .then(
                                                if (compactNavigation)
                                                        Modifier.horizontalScroll(scrollState)
                                                else Modifier
                                        ),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                        items.forEachIndexed { index, title ->
                                val selected = index == selectedIndex
                                val sizingModifier =
                                        if (compactNavigation) {
                                                Modifier.defaultMinSize(minWidth = 76.dp)
                                        } else Modifier.weight(1f)
                                Box(
                                        modifier =
                                                sizingModifier
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(
                                                                if (selected) ElevatedBg
                                                                else Color.Transparent
                                                        )
                                                        .border(
                                                                width =
                                                                        if (selected) 0.5.dp
                                                                        else 0.dp,
                                                                color =
                                                                        if (selected)
                                                                                Color(0xFF334155)
                                                                        else Color.Transparent,
                                                                shape = RoundedCornerShape(9.dp)
                                                        )
                                                        .clickable { onSelect(index) }
                                                        .padding(
                                                                horizontal = 6.dp,
                                                                vertical = 9.dp
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(
                                                text = title,
                                                fontSize = 12.sp,
                                                fontWeight =
                                                        if (selected) FontWeight.Bold
                                                        else FontWeight.Medium,
                                                color = if (selected) AccentCyan else TextMuted,
                                                maxLines = 1
                                        )
                                }
                        }
                }
        }
}

@Composable
fun SectionPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
        Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = PanelBg,
                border = BorderStroke(1.dp, HairlineBorder)
        ) { Column(modifier = Modifier.padding(16.dp), content = content) }
}

@Composable
private fun FocusGuardMark(size: Dp, modifier: Modifier = Modifier) {
        val shape = RoundedCornerShape(12.dp)
        Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier =
                        modifier.size(size)
                                .clip(shape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                                .padding(size * 0.1f)
        )
}

@Composable
private fun EmptyAppResults(searchQuery: String) {
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .heightIn(min = 148.dp)
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
                FocusGuardMark(size = 48.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                        text =
                                if (searchQuery.isBlank()) "No apps to show"
                                else "No apps match your search",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMuted
                )
        }
}

@Composable
private fun AppIconBadge(packageName: String, appLabel: String, size: Dp = 40.dp) {
        val context = LocalContext.current
        var icon by
                remember(packageName) {
                        mutableStateOf(AppIconBitmapCache.cached(packageName)?.asImageBitmap())
                }
        LaunchedEffect(packageName) {
                if (icon == null) {
                        icon = AppIconBitmapCache.load(context, packageName)?.asImageBitmap()
                }
        }
        val shape = RoundedCornerShape(12.dp)
        Box(
                modifier =
                        Modifier.size(size)
                                .clip(shape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
                contentAlignment = Alignment.Center
        ) {
                icon?.let { loadedIcon ->
                        Image(
                                bitmap = loadedIcon,
                                contentDescription = "$appLabel icon",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(4.dp)
                        )
                }
                        ?: run { FocusGuardMark(size = size * 0.62f) }
        }
}

@Composable
fun MindfulPauseCard(targetPackage: String, onCancel: () -> Unit, onProceed: () -> Unit) {
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
                                Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                                        Text("Back to Study")
                                }
                                OutlinedButton(
                                        onClick = onProceed,
                                        enabled = secondsLeft == 0,
                                        modifier = Modifier.weight(1f)
                                ) {
                                        Text(
                                                text =
                                                        if (secondsLeft > 0) "Wait ${secondsLeft}s"
                                                        else "Open (5m Pass)",
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
        appModesLocked: Boolean,
        requestConfirmation: ConfirmationRequester,
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

        val filteredApps =
                remember(searchQuery, installedApps, blockedSet, pausedSet) {
                        val matching =
                                if (searchQuery.isBlank()) {
                                        installedApps
                                } else {
                                        installedApps.filter {
                                                it.label.contains(searchQuery, ignoreCase = true) ||
                                                        it.packageName.contains(
                                                                searchQuery,
                                                                ignoreCase = true
                                                        )
                                        }
                                }

                        matching.sortedWith(
                                compareBy<AppEntry> { app ->
                                        when {
                                                blockedSet.contains(app.packageName) ->
                                                        0 // Blocked apps at the very top
                                                pausedSet.contains(app.packageName) ->
                                                        1 // 15s Paused apps next
                                                else -> 2 // Open apps below
                                        }
                                }
                                        .thenBy { it.label.lowercase() }
                        )
                }

        Column(modifier = Modifier.fillMaxSize()) {
                SectionPanel {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                "Block YouTube Shorts & IG Reels",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                        )
                                        Text(
                                                "Keeps regular videos & DMs open",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                        )
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
                        label = {
                                Text(
                                        "Search apps (${blockedSet.size} blocked, ${pausedSet.size} paused)"
                                )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (filteredApps.isEmpty()) {
                                item(key = "empty-app-results") { EmptyAppResults(searchQuery) }
                        } else
                                items(filteredApps, key = { it.packageName }) { app ->
                                        val currentMode =
                                                when {
                                                        blockedSet.contains(app.packageName) ->
                                                                FocusPrefs.APP_MODE_BLOCKED
                                                        pausedSet.contains(app.packageName) ->
                                                                FocusPrefs.APP_MODE_PAUSE
                                                        else -> FocusPrefs.APP_MODE_OPEN
                                                }

                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                AppIconBadge(
                                                        packageName = app.packageName,
                                                        appLabel = app.label
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(
                                                        modifier =
                                                                Modifier.weight(1f)
                                                                        .padding(end = 6.dp)
                                                ) {
                                                        Text(
                                                                text = app.label,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                fontWeight = FontWeight.Medium,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                        )
                                                        val modeDescription =
                                                                when (currentMode) {
                                                                        FocusPrefs
                                                                                .APP_MODE_BLOCKED ->
                                                                                "Blocked"
                                                                        FocusPrefs.APP_MODE_PAUSE ->
                                                                                "Mindful pause · 15s"
                                                                        else -> "Open"
                                                                }
                                                        val modeDescriptionColor =
                                                                when (currentMode) {
                                                                        FocusPrefs
                                                                                .APP_MODE_BLOCKED ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error
                                                                        FocusPrefs.APP_MODE_PAUSE ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .tertiary
                                                                        else -> TextMuted
                                                                }
                                                        Text(
                                                                text = modeDescription,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
                                                                color = modeDescriptionColor,
                                                                maxLines = 1
                                                        )
                                                }
                                                Row(
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(4.dp)
                                                ) {
                                                        val modes =
                                                                listOf(
                                                                        FocusPrefs.APP_MODE_OPEN to
                                                                                "Open",
                                                                        FocusPrefs.APP_MODE_PAUSE to
                                                                                "15s",
                                                                        FocusPrefs
                                                                                .APP_MODE_BLOCKED to
                                                                                "Block"
                                                                )
                                                        modes.forEach { (mode, label) ->
                                                                val selected = currentMode == mode
                                                                val canSelect =
                                                                        !appModesLocked ||
                                                                                mode >= currentMode
                                                                val chipBg =
                                                                        when {
                                                                                selected &&
                                                                                        mode ==
                                                                                                FocusPrefs
                                                                                                        .APP_MODE_BLOCKED ->
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .errorContainer
                                                                                selected &&
                                                                                        mode ==
                                                                                                FocusPrefs
                                                                                                        .APP_MODE_PAUSE ->
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .tertiaryContainer
                                                                                selected ->
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primaryContainer
                                                                                else ->
                                                                                        Color.Transparent
                                                                        }
                                                                val chipText =
                                                                        when {
                                                                                selected &&
                                                                                        mode ==
                                                                                                FocusPrefs
                                                                                                        .APP_MODE_BLOCKED ->
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onErrorContainer
                                                                                selected &&
                                                                                        mode ==
                                                                                                FocusPrefs
                                                                                                        .APP_MODE_PAUSE ->
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onTertiaryContainer
                                                                                selected ->
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onPrimaryContainer
                                                                                canSelect ->
                                                                                        TextMuted
                                                                                else ->
                                                                                        TextMuted
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.55f
                                                                                                )
                                                                        }
                                                                Box(
                                                                        modifier =
                                                                                Modifier.clip(
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                        )
                                                                                        .background(
                                                                                                chipBg
                                                                                        )
                                                                                        .border(
                                                                                                1.dp,
                                                                                                if (selected
                                                                                                )
                                                                                                        chipText.copy(
                                                                                                                alpha =
                                                                                                                        0.6f
                                                                                                        )
                                                                                                else
                                                                                                        HairlineBorder,
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                        )
                                                                                        .clickable(
                                                                                                enabled =
                                                                                                        canSelect
                                                                                        ) {
                                                                                                val applyMode =
                                                                                                        {
                                                                                                                FocusPrefs
                                                                                                                        .setAppMode(
                                                                                                                                context,
                                                                                                                                app.packageName,
                                                                                                                                mode
                                                                                                                        )
                                                                                                                blockedSet =
                                                                                                                        FocusPrefs
                                                                                                                                .getBlockedPackages(
                                                                                                                                        context
                                                                                                                                )
                                                                                                                pausedSet =
                                                                                                                        FocusPrefs
                                                                                                                                .getPausedPackages(
                                                                                                                                        context
                                                                                                                                )
                                                                                                        }
                                                                                                if (mode >
                                                                                                                currentMode
                                                                                                ) {
                                                                                                        val effect =
                                                                                                                if (mode ==
                                                                                                                                FocusPrefs
                                                                                                                                        .APP_MODE_BLOCKED
                                                                                                                ) {
                                                                                                                        "${app.label} will be blocked while its rule is active."
                                                                                                                } else {
                                                                                                                        "Opening ${app.label} will require a 15-second pause before a temporary pass."
                                                                                                                }
                                                                                                        requestConfirmation(
                                                                                                                "Apply ${if (mode == FocusPrefs.APP_MODE_BLOCKED) "Block" else "15-second Pause"} to ${app.label}?",
                                                                                                                effect,
                                                                                                                "Apply rule",
                                                                                                                applyMode
                                                                                                        )
                                                                                                } else
                                                                                                        applyMode()
                                                                                        }
                                                                                        .defaultMinSize(
                                                                                                minWidth =
                                                                                                        44.dp,
                                                                                                minHeight =
                                                                                                        48.dp
                                                                                        )
                                                                                        .padding(
                                                                                                horizontal =
                                                                                                        8.dp,
                                                                                                vertical =
                                                                                                        4.dp
                                                                                        ),
                                                                        contentAlignment =
                                                                                Alignment.Center
                                                                ) {
                                                                        Text(
                                                                                text = label,
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .labelSmall,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .SemiBold,
                                                                                color = chipText
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                        HorizontalDivider(
                                                thickness = 0.5.dp,
                                                color = HairlineBorder
                                        )
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
        var customDomains by remember {
                mutableStateOf(FocusPrefs.getCustomBlockedDomains(context).sorted())
        }

        Column(modifier = Modifier.fillMaxSize()) {
                SectionPanel {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                "Block Adult Contents",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                        )
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
                                onClick = {
                                        context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("Open System VPN Settings", fontSize = 12.sp) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val stackDomainEntry =
                                maxWidth < 390.dp ||
                                        androidx.compose.ui.platform.LocalDensity.current
                                                .fontScale > 1.25f
                        val addDomain = {
                                FocusPrefs.addBlockedDomain(context, domainInput)
                                customDomains = FocusPrefs.getCustomBlockedDomains(context).sorted()
                                domainInput = ""
                        }
                        if (stackDomainEntry) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                                value = domainInput,
                                                onValueChange = { domainInput = it },
                                                label = { Text("Block domain or *wildcard*") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                        )
                                        Button(
                                                onClick = addDomain,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                        ) { Text("Block domain") }
                                }
                        } else {
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
                                                onClick = addDomain,
                                                shape = RoundedCornerShape(10.dp)
                                        ) { Text("Block") }
                                }
                        }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(customDomains, key = { it }) { domain ->
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                domain,
                                                fontSize = 14.sp,
                                                modifier = Modifier.weight(1f)
                                        )
                                        if (!isLocked) {
                                                TextButton(
                                                        onClick = {
                                                                FocusPrefs.removeBlockedDomain(
                                                                        context,
                                                                        domain
                                                                )
                                                                customDomains =
                                                                        FocusPrefs
                                                                                .getCustomBlockedDomains(
                                                                                        context
                                                                                )
                                                                                .sorted()
                                                        }
                                                ) { Text("Remove", fontSize = 12.sp) }
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
        pomoRemainingMs: Long,
        requestConfirmation: ConfirmationRequester
) {
        val context = LocalContext.current
        var scheduleEnabled by remember { mutableStateOf(FocusPrefs.isScheduleEnabled(context)) }
        var startMin by remember { mutableIntStateOf(FocusPrefs.getScheduleStartMinutes(context)) }
        var endMin by remember { mutableIntStateOf(FocusPrefs.getScheduleEndMinutes(context)) }
        var clockTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var pendingTimeChange by remember { mutableStateOf<PendingScheduleTimeChange?>(null) }
        var notificationReadiness by remember { mutableStateOf(FocusNotifier.readiness(context)) }
        var showNotificationRationale by remember { mutableStateOf(false) }

        val notificationPermissionLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                ) { notificationReadiness = FocusNotifier.readiness(context) }

        val askForFocusAlerts = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !notificationReadiness.permissionGranted
                ) {
                        showNotificationRationale = true
                }
        }

        var scheduledPackages by remember {
                mutableStateOf(FocusPrefs.getScheduledPackages(context))
        }
        var scheduleSearch by remember { mutableStateOf("") }

        var scheduleApps by remember { mutableStateOf(LaunchableAppInventory.cachedApps()) }
        var appInventoryFailed by remember { mutableStateOf(false) }
        var appInventoryRetry by remember { mutableIntStateOf(0) }

        LaunchedEffect(appInventoryRetry) {
                if (scheduleApps == null) {
                        appInventoryFailed = false
                        try {
                                scheduleApps = LaunchableAppInventory.load(context)
                        } catch (error: Exception) {
                                if (error is kotlinx.coroutines.CancellationException) throw error
                                appInventoryFailed = true
                        }
                }
        }

        // Selected apps pinned at top alphabetically, followed by unselected apps alphabetically
        val filteredScheduleApps =
                remember(scheduleApps, scheduleSearch, scheduledPackages) {
                        val availableApps = scheduleApps.orEmpty()
                        val matching =
                                if (scheduleSearch.isBlank()) {
                                        availableApps
                                } else {
                                        availableApps.filter {
                                                it.label.contains(
                                                        scheduleSearch,
                                                        ignoreCase = true
                                                ) ||
                                                        it.packageName.contains(
                                                                scheduleSearch,
                                                                ignoreCase = true
                                                        )
                                        }
                                }
                        matching.sortedWith(
                                compareBy<AppEntry> { it.packageName !in scheduledPackages }
                                        .thenBy { it.label.lowercase() }
                        )
                }

        var selectedPomoMins by remember { mutableIntStateOf(50) }
        var lockDuringPomo by remember { mutableStateOf(FocusPrefs.isPomodoroLockEnabled(context)) }
        val todayMins = FocusPrefs.getTodayFocusedMinutes(context)
        val todaySessions = FocusPrefs.getTodaySessions(context)

        LaunchedEffect(Unit) {
                while (true) {
                        delay(15_000L)
                        clockTick = System.currentTimeMillis()
                        notificationReadiness = FocusNotifier.readiness(context)
                }
        }

        val scheduleActive =
                remember(scheduleEnabled, startMin, endMin, clockTick) {
                        FocusPrefs.isStudyWindowCurrentlyActive(context)
                }

        val independentlyLocked =
                FocusPrefs.isStrictLockActive(context) ||
                        FocusPrefs.isPomodoroLockCurrentlyActive(context)

        // Users may add apps during an active Daily Auto-Lock window, but selected apps stay in the
        // schedule until the window ends. Other active protection locks also prevent removal.
        fun changeScheduleMembership(app: AppEntry, selected: Boolean) {
                if (!selected && (scheduleActive || independentlyLocked)) return

                val updated =
                        scheduledPackages.toMutableSet().apply {
                                if (selected) add(app.packageName) else remove(app.packageName)
                        }
                val pendingChange =
                        PendingScheduleMembershipChange(
                                nextPackages = updated,
                                persist = { packages ->
                                        FocusPrefs.setScheduledPackages(context, packages)
                                        scheduledPackages = FocusPrefs.getScheduledPackages(context)
                                },
                                reevaluateForeground = {
                                        FocusAccessibilityService.requestActiveWindowReevaluation()
                                }
                        )
                val apply = { pendingChange.confirm() }
                if (scheduleActive && selected) {
                        requestConfirmation(
                                "Add ${app.label} to the active schedule?",
                                "${app.label} will be locked by Daily Auto-Lock immediately while this window is active.",
                                "Add app",
                                apply
                        )
                } else {
                        apply()
                }
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exactAlarmsAvailable =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        alarmManager.canScheduleExactAlarms()

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isCompact = maxWidth < 380.dp
                val sectionGap = if (isCompact) 10.dp else 12.dp
                val timerFontSize = if (isCompact) 28.sp else 34.sp
                val iconSize = if (isCompact) 32.dp else 36.dp
                val listMaxHeight = if (isCompact) 210.dp else 260.dp

                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(sectionGap),
                        contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                        // 1. Deep-Work Study Timer Card
                        item(key = "pomo-section") {
                                SectionPanel {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        text = "Deep-Work Study Timer",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier =
                                                                Modifier.weight(1f)
                                                                        .padding(end = 8.dp)
                                                )
                                                Text(
                                                        text =
                                                                "Today: ${todayMins}m ($todaySessions)",
                                                        style =
                                                                MaterialTheme.typography.labelMedium
                                                                        .copy(
                                                                                fontFeatureSettings =
                                                                                        "tnum"
                                                                        ),
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = AccentCyan,
                                                        maxLines = 1
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        if (pomoRemainingMs > 0L) {
                                                Text(
                                                        text = formatDuration(pomoRemainingMs),
                                                        fontSize = timerFontSize,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        style =
                                                                TextStyle(
                                                                        fontFeatureSettings = "tnum"
                                                                ),
                                                        color = AccentCyan
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                if (!lockDuringPomo) {
                                                        OutlinedButton(
                                                                onClick = {
                                                                        FocusPrefs.stopPomodoro(
                                                                                context
                                                                        )
                                                                },
                                                                shape = RoundedCornerShape(10.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                        ) { Text("Stop Timer") }
                                                } else {
                                                        Text(
                                                                text =
                                                                        "Distractions & Settings locked until session completes.",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color = TextMuted
                                                        )
                                                }
                                        } else {
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(
                                                                        if (isCompact) 6.dp
                                                                        else 8.dp
                                                                )
                                                ) {
                                                        listOf(25, 50, 90).forEach { mins ->
                                                                val active =
                                                                        selectedPomoMins == mins
                                                                if (active) {
                                                                        Button(
                                                                                onClick = {
                                                                                        selectedPomoMins =
                                                                                                mins
                                                                                },
                                                                                shape =
                                                                                        RoundedCornerShape(
                                                                                                10.dp
                                                                                        ),
                                                                                contentPadding =
                                                                                        PaddingValues(
                                                                                                horizontal =
                                                                                                        6.dp,
                                                                                                vertical =
                                                                                                        8.dp
                                                                                        ),
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        )
                                                                        ) {
                                                                                Text(
                                                                                        "${mins}m",
                                                                                        maxLines = 1
                                                                                )
                                                                        }
                                                                } else {
                                                                        OutlinedButton(
                                                                                onClick = {
                                                                                        selectedPomoMins =
                                                                                                mins
                                                                                },
                                                                                shape =
                                                                                        RoundedCornerShape(
                                                                                                10.dp
                                                                                        ),
                                                                                contentPadding =
                                                                                        PaddingValues(
                                                                                                horizontal =
                                                                                                        6.dp,
                                                                                                vertical =
                                                                                                        8.dp
                                                                                        ),
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        )
                                                                        ) {
                                                                                Text(
                                                                                        "${mins}m",
                                                                                        maxLines = 1
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text =
                                                                        "Lock distractions during session",
                                                                modifier =
                                                                        Modifier.weight(1f)
                                                                                .padding(
                                                                                        end = 8.dp
                                                                                ),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color = TextMuted
                                                        )
                                                        Switch(
                                                                checked = lockDuringPomo,
                                                                onCheckedChange = {
                                                                        val apply = {
                                                                                lockDuringPomo = it
                                                                                FocusPrefs
                                                                                        .setPomodoroLockEnabled(
                                                                                                context,
                                                                                                it
                                                                                        )
                                                                        }
                                                                        if (it && !lockDuringPomo) {
                                                                                requestConfirmation(
                                                                                        "Lock distractions during Pomodoro?",
                                                                                        "Selected app blocks will apply while a study timer is running.",
                                                                                        "Enable lock",
                                                                                        apply
                                                                                )
                                                                        } else apply()
                                                                }
                                                        )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Button(
                                                        onClick = {
                                                                val startSession = {
                                                                        FocusPrefs.startPomodoro(
                                                                                context,
                                                                                selectedPomoMins
                                                                        )
                                                                }
                                                                if (lockDuringPomo) {
                                                                        requestConfirmation(
                                                                                "Start a locked study session?",
                                                                                "FocusGuard will enforce your selected app rules for ${selectedPomoMins} minutes.",
                                                                                "Start session",
                                                                                startSession
                                                                        )
                                                                } else startSession()
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                ) {
                                                        Text(
                                                                text =
                                                                        "Start ${selectedPomoMins}-Minute Study Block",
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                        )
                                                }
                                        }
                                }
                        }

                        // 2. Daily Auto-Lock Window & App Selector
                        item(key = "schedule-section") {
                                SectionPanel {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Column(
                                                        modifier =
                                                                Modifier.weight(1f)
                                                                        .padding(end = 8.dp)
                                                ) {
                                                        Text(
                                                                text = "Daily Auto-Lock Window",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleSmall,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                                text =
                                                                        "Automatically enforces blocks & settings during study hours",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color = TextMuted
                                                        )
                                                }
                                                Switch(
                                                        checked = scheduleEnabled,
                                                        enabled = !isLocked || !scheduleEnabled,
                                                        onCheckedChange = { enabled ->
                                                                if (!enabled && isLocked)
                                                                        return@Switch
                                                                val apply = {
                                                                        scheduleEnabled = enabled
                                                                        FocusPrefs
                                                                                .setScheduleEnabled(
                                                                                        context,
                                                                                        enabled
                                                                                )
                                                                        if (enabled)
                                                                                askForFocusAlerts()
                                                                }
                                                                if (enabled && !scheduleEnabled) {
                                                                        requestConfirmation(
                                                                                "Enable Daily Auto-Lock?",
                                                                                "${scheduledPackages.size} selected app(s) will be blocked from ${formatMinuteOfDay(startMin)} to ${formatMinuteOfDay(endMin)} each day.",
                                                                                "Enable schedule",
                                                                                apply
                                                                        )
                                                                } else apply()
                                                        }
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        TimeAdjustRow(
                                                label = "Start Time",
                                                minutesFromMidnight = startMin,
                                                enabled = !isLocked,
                                                onAdjust = { delta ->
                                                        FocusPrefs.adjustScheduleMinutes(
                                                                context,
                                                                isStart = true,
                                                                deltaMinutes = delta
                                                        )
                                                        startMin =
                                                                FocusPrefs.getScheduleStartMinutes(
                                                                        context
                                                                )
                                                },
                                                onTimeSelected = { minute ->
                                                        pendingTimeChange =
                                                                PendingScheduleTimeChange(
                                                                        isStart = true,
                                                                        proposedMinutes = minute
                                                                ) { isStart, value ->
                                                                        FocusPrefs
                                                                                .setScheduleTimeMinutes(
                                                                                        context,
                                                                                        isStart =
                                                                                                isStart,
                                                                                        minutes =
                                                                                                value
                                                                                )
                                                                        startMin =
                                                                                FocusPrefs
                                                                                        .getScheduleStartMinutes(
                                                                                                context
                                                                                        )
                                                                }
                                                }
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        TimeAdjustRow(
                                                label = "End Time",
                                                minutesFromMidnight = endMin,
                                                enabled = !isLocked,
                                                onAdjust = { delta ->
                                                        FocusPrefs.adjustScheduleMinutes(
                                                                context,
                                                                isStart = false,
                                                                deltaMinutes = delta
                                                        )
                                                        endMin =
                                                                FocusPrefs.getScheduleEndMinutes(
                                                                        context
                                                                )
                                                },
                                                onTimeSelected = { minute ->
                                                        pendingTimeChange =
                                                                PendingScheduleTimeChange(
                                                                        isStart = false,
                                                                        proposedMinutes = minute
                                                                ) { isStart, value ->
                                                                        FocusPrefs
                                                                                .setScheduleTimeMinutes(
                                                                                        context,
                                                                                        isStart =
                                                                                                isStart,
                                                                                        minutes =
                                                                                                value
                                                                                )
                                                                        endMin =
                                                                                FocusPrefs
                                                                                        .getScheduleEndMinutes(
                                                                                                context
                                                                                        )
                                                                }
                                                }
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                                text =
                                                        "Apps locked only during this window (${scheduledPackages.size} selected)",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                text =
                                                        "Selected apps appear at the top. Outside this window, schedule-only apps stay open.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                                value = scheduleSearch,
                                                onValueChange = { scheduleSearch = it },
                                                label = { Text("Search installed apps") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Keep the list bounded and compose rows and icons only
                                        // when they are visible.
                                        LazyColumn(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .heightIn(
                                                                        min = 64.dp,
                                                                        max = listMaxHeight
                                                                ),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                                if (scheduleApps == null && appInventoryFailed) {
                                                        item(key = "app-inventory-error") {
                                                                Column(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .padding(
                                                                                                16.dp
                                                                                        ),
                                                                        horizontalAlignment =
                                                                                Alignment
                                                                                        .CenterHorizontally
                                                                ) {
                                                                        Text(
                                                                                "Installed apps could not be loaded.",
                                                                                color = TextMuted
                                                                        )
                                                                        TextButton(
                                                                                onClick = {
                                                                                        appInventoryRetry++
                                                                                }
                                                                        ) { Text("Try again") }
                                                                }
                                                        }
                                                } else if (scheduleApps == null) {
                                                        item(key = "app-inventory-loading") {
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .padding(
                                                                                                18.dp
                                                                                        ),
                                                                        horizontalArrangement =
                                                                                Arrangement.Center,
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        CircularProgressIndicator(
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                20.dp
                                                                                        ),
                                                                                strokeWidth = 2.dp
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                10.dp
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                "Loading installed apps…",
                                                                                color = TextMuted
                                                                        )
                                                                }
                                                        }
                                                } else if (filteredScheduleApps.isEmpty()) {
                                                        item(key = "app-inventory-empty") {
                                                                EmptyAppResults(scheduleSearch)
                                                        }
                                                } else {
                                                        items(
                                                                filteredScheduleApps,
                                                                key = AppEntry::packageName
                                                        ) { app ->
                                                                val selected =
                                                                        app.packageName in
                                                                                scheduledPackages
                                                                // Additions remain available during
                                                                // the active window; selected
                                                                // entries cannot be removed until
                                                                // it ends.
                                                                val canToggleApp =
                                                                        !selected ||
                                                                                (!scheduleActive &&
                                                                                        !independentlyLocked)
                                                                val rowShape =
                                                                        RoundedCornerShape(10.dp)
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .clip(
                                                                                                rowShape
                                                                                        )
                                                                                        .background(
                                                                                                if (selected
                                                                                                )
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primaryContainer
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.38f
                                                                                                                )
                                                                                                else
                                                                                                        Color.Transparent
                                                                                        )
                                                                                        .border(
                                                                                                1.dp,
                                                                                                if (selected
                                                                                                )
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primary
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.5f
                                                                                                                )
                                                                                                else
                                                                                                        HairlineBorder
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.45f
                                                                                                                ),
                                                                                                rowShape
                                                                                        )
                                                                                        .clickable(
                                                                                                enabled =
                                                                                                        canToggleApp
                                                                                        ) {
                                                                                                changeScheduleMembership(
                                                                                                        app,
                                                                                                        selected =
                                                                                                                !selected
                                                                                                )
                                                                                        }
                                                                                        .defaultMinSize(
                                                                                                minHeight =
                                                                                                        if (isCompact
                                                                                                        )
                                                                                                                52.dp
                                                                                                        else
                                                                                                                56.dp
                                                                                        )
                                                                                        .padding(
                                                                                                horizontal =
                                                                                                        if (isCompact
                                                                                                        )
                                                                                                                6.dp
                                                                                                        else
                                                                                                                8.dp,
                                                                                                vertical =
                                                                                                        4.dp
                                                                                        ),
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        AppIconBadge(
                                                                                packageName =
                                                                                        app.packageName,
                                                                                appLabel =
                                                                                        app.label,
                                                                                size = iconSize
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                if (isCompact
                                                                                                )
                                                                                                        6.dp
                                                                                                else
                                                                                                        8.dp
                                                                                        )
                                                                        )
                                                                        Checkbox(
                                                                                checked = selected,
                                                                                enabled =
                                                                                        canToggleApp,
                                                                                onCheckedChange = {
                                                                                        checked ->
                                                                                        if (canToggleApp
                                                                                        )
                                                                                                changeScheduleMembership(
                                                                                                        app,
                                                                                                        checked
                                                                                                )
                                                                                }
                                                                        )
                                                                        Column(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                        1f
                                                                                                )
                                                                                                .padding(
                                                                                                        start =
                                                                                                                2.dp
                                                                                                )
                                                                        ) {
                                                                                Text(
                                                                                        text =
                                                                                                app.label,
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .bodyMedium,
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .Medium,
                                                                                        maxLines =
                                                                                                1,
                                                                                        overflow =
                                                                                                TextOverflow
                                                                                                        .Ellipsis
                                                                                )
                                                                                Text(
                                                                                        text =
                                                                                                when {
                                                                                                        selected &&
                                                                                                                scheduleActive ->
                                                                                                                "Locked during active window"
                                                                                                        selected ->
                                                                                                                "Selected · follows Daily Auto-Lock"
                                                                                                        scheduleEnabled ->
                                                                                                                "Tap to add to Auto-Lock"
                                                                                                        else ->
                                                                                                                "Not selected"
                                                                                                },
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .labelSmall,
                                                                                        color =
                                                                                                if (selected
                                                                                                )
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primary
                                                                                                else
                                                                                                        TextMuted,
                                                                                        maxLines =
                                                                                                1,
                                                                                        overflow =
                                                                                                TextOverflow
                                                                                                        .Ellipsis
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                                text =
                                                        if (scheduleActive)
                                                                "Status: Window active — scheduled apps are locked."
                                                        else
                                                                "Status: Window inactive — scheduled apps are usable unless another rule applies.",
                                                color =
                                                        if (scheduleActive) AccentAmber
                                                        else TextMuted,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                                text =
                                                        "Start is included, end is excluded. Overnight windows are supported; matching start and end times mean all day.",
                                                color = TextMuted,
                                                style = MaterialTheme.typography.bodySmall
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        StatusLine(
                                                "Precise schedule boundaries",
                                                exactAlarmsAvailable
                                        )
                                        if (!exactAlarmsAvailable &&
                                                        Build.VERSION.SDK_INT >=
                                                                Build.VERSION_CODES.S
                                        ) {
                                                Text(
                                                        text =
                                                                "Alarms & reminders access lets FocusGuard deliver exact start/end transitions and alerts.",
                                                        color = TextMuted,
                                                        style = MaterialTheme.typography.bodySmall
                                                )
                                                TextButton(
                                                        onClick = {
                                                                context.startActivity(
                                                                        Intent(
                                                                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                                                                )
                                                                                .setData(
                                                                                        Uri.parse(
                                                                                                "package:${context.packageName}"
                                                                                        )
                                                                                )
                                                                )
                                                        }
                                                ) { Text("Allow precise schedule alerts") }
                                        }
                                        StatusLine(
                                                "Daily Auto-Lock alerts",
                                                notificationReadiness.scheduleAlertsAvailable
                                        )
                                        StatusLine(
                                                "App-block alerts",
                                                notificationReadiness.appBlockAlertsAvailable
                                        )
                                        StatusLine(
                                                "Comic focus nudges",
                                                notificationReadiness.nudgeAlertsAvailable
                                        )

                                        if (!notificationReadiness.permissionGranted &&
                                                        Build.VERSION.SDK_INT >=
                                                                Build.VERSION_CODES.TIRAMISU
                                        ) {
                                                TextButton(
                                                        onClick = {
                                                                showNotificationRationale = true
                                                        }
                                                ) { Text("Enable focus notifications") }
                                        }
                                        if (!notificationReadiness.appEnabled ||
                                                        !notificationReadiness
                                                                .scheduleChannelEnabled ||
                                                        !notificationReadiness
                                                                .blockChannelEnabled ||
                                                        !notificationReadiness.nudgeChannelEnabled
                                        ) {
                                                TextButton(
                                                        onClick = {
                                                                context.startActivity(
                                                                        Intent(
                                                                                        Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                                                                )
                                                                                .putExtra(
                                                                                        Settings.EXTRA_APP_PACKAGE,
                                                                                        context.packageName
                                                                                )
                                                                )
                                                        }
                                                ) { Text("Open notification settings") }
                                        }
                                }
                        }

                        // 3. Comic Focus Nudges Card
                        item(key = "comic-nudges-section") {
                                var comicNudges by remember {
                                        mutableStateOf(FocusPrefs.isComicNudgesEnabled(context))
                                }
                                SectionPanel {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Column(
                                                        modifier =
                                                                Modifier.weight(1f)
                                                                        .padding(end = 8.dp)
                                                ) {
                                                        Text(
                                                                text = "Comic Focus Nudges",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleSmall,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                                text =
                                                                        "Sends witty reminders if you linger 12–20m on distracting apps",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color = TextMuted
                                                        )
                                                }
                                                Switch(
                                                        checked = comicNudges,
                                                        onCheckedChange = {
                                                                comicNudges = it
                                                                FocusPrefs.setComicNudgesEnabled(
                                                                        context,
                                                                        it
                                                                )
                                                                if (it) askForFocusAlerts()
                                                        }
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                                onClick = {
                                                        FocusNotifier.sendComicSessionNudge(
                                                                context,
                                                                15
                                                        )
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                        ) {
                                                Text(
                                                        "Preview Comic Notification",
                                                        style = MaterialTheme.typography.labelLarge
                                                )
                                        }
                                }
                        }
                }

                if (showNotificationRationale) {
                        ResponsivePromptDialog(
                                title = "Allow focus notifications?",
                                message =
                                        "FocusGuard can notify you when the Daily Auto-Lock Window starts or ends and when an app is blocked. Alerts are optional; app blocking keeps working if you decline.",
                                confirmLabel = "Continue",
                                dismissLabel = "Not now",
                                onDismiss = { showNotificationRationale = false },
                                onConfirm = {
                                        showNotificationRationale = false
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notificationPermissionLauncher.launch(
                                                        android.Manifest.permission
                                                                .POST_NOTIFICATIONS
                                                )
                                        }
                                }
                        )
                }

                pendingTimeChange?.let { change ->
                        val boundaryName = if (change.isStart) "Start" else "End"
                        ResponsivePromptDialog(
                                title = "Confirm $boundaryName Time",
                                message =
                                        "Set the Daily Auto-Lock $boundaryName Time to ${formatMinuteOfDay(change.proposedMinutes)}?",
                                confirmLabel = "Set time",
                                onDismiss = {
                                        change.cancel()
                                        pendingTimeChange = null
                                },
                                onConfirm = {
                                        pendingTimeChange = null
                                        change.confirm()
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
        onAdjust: (Int) -> Unit,
        onTimeSelected: (Int) -> Unit
) {
        val context = LocalContext.current
        val hours = minutesFromMidnight / 60
        val mins = minutesFromMidnight % 60
        val formatted = String.format("%02d:%02d", hours, mins)

        Column(modifier = Modifier.fillMaxWidth()) {
                Text(label, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                        OutlinedButton(
                                onClick = { onAdjust(-30) },
                                enabled = enabled,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                        ) { Text("-30m", fontSize = 12.sp) }
                        Button(
                                onClick = {
                                        TimePickerDialog(
                                                        context,
                                                        { _, hour, minute ->
                                                                onTimeSelected(hour * 60 + minute)
                                                        },
                                                        hours,
                                                        mins,
                                                        DateFormat.is24HourFormat(context)
                                                )
                                                .show()
                                },
                                enabled = enabled,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                        ) { Text(formatted, style = TextStyle(fontFeatureSettings = "tnum")) }
                        OutlinedButton(
                                onClick = { onAdjust(30) },
                                enabled = enabled,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                        ) { Text("+30m", fontSize = 12.sp) }
                }
        }
}

@Composable
fun ShieldLockTab(
        isServiceEnabled: Boolean,
        isDeviceAdminEnabled: Boolean,
        vpnActive: Boolean,
        lockState: Int,
        remainingMs: Long,
        requestConfirmation: ConfirmationRequester,
        onUpdateFound: (ReleaseUpdate) -> Unit,
        onLockStateChanged: () -> Unit
) {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val scope = rememberCoroutineScope()
        val installedVersion = remember { UpdateChecker.getInstalledVersion(context) }

        var updateStatusText by remember { mutableStateOf<String?>(null) }
        var isCheckingUpdate by remember { mutableStateOf(false) }

        val presets = listOf(15 to "15m", 60 to "1h", 360 to "6h", 1440 to "24h", 10080 to "7d")
        var selectedMinutes by remember { mutableIntStateOf(1440) }

        fun reportIssue() {
                val title =
                        Uri.encode(
                                "[Bug Report] v$installedVersion on ${Build.MANUFACTURER} ${Build.MODEL}"
                        )
                val body =
                        Uri.encode(
                                "### Describe the issue\n(Write what happened here)\n\n" +
                                        "---\n### Device Diagnostics\n" +
                                        "- **App Version:** v$installedVersion\n" +
                                        "- **Device:** ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                                        "- **Android SDK:** ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                                        "- **Accessibility Service:** ${if (isServiceEnabled) "ACTIVE" else "OFF"}\n" +
                                        "- **Local DNS Shield:** ${if (vpnActive) "ACTIVE" else "OFF"}\n" +
                                        "- **Device Admin:** ${if (isDeviceAdminEnabled) "ACTIVE" else "OFF"}\n"
                        )
                uriHandler.openUri(
                        "https://github.com/quasimkhan/FocusGuard/issues/new?title=$title&body=$body"
                )
        }

        fun checkForUpdates() {
                if (isCheckingUpdate) return
                isCheckingUpdate = true
                updateStatusText = "Checking GitHub..."
                scope.launch {
                        when (val result = UpdateChecker.checkForUpdate(context, force = true)) {
                                is UpdateCheckResult.UpdateAvailable -> {
                                        updateStatusText =
                                                "New release ${result.update.latestTag} available!"
                                        onUpdateFound(result.update)
                                }
                                is UpdateCheckResult.UpToDate ->
                                        updateStatusText = "Up to date (v${result.currentVersion})"
                                is UpdateCheckResult.Error -> updateStatusText = result.reason
                        }
                        isCheckingUpdate = false
                }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                SectionPanel {
                        Text("System Armor Status", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusLine("Accessibility & Settings Watchdog", isServiceEnabled)
                        StatusLine("Local DNS & Mirror Firewall", vpnActive)
                        StatusLine("Device Admin deactivation warning", isDeviceAdminEnabled)
                        Text(
                                "A regular Device Admin cannot guarantee uninstall prevention. While a focus lock is active, the Accessibility Service can dismiss a clearly identified FocusGuard uninstall confirmation; some OEM dialogs may not expose enough information.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                        )

                        if (!isDeviceAdminEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                        onClick = {
                                                val adminComponent =
                                                        ComponentName(
                                                                context,
                                                                FocusDeviceAdminReceiver::class.java
                                                        )
                                                val intent =
                                                        Intent(
                                                                        DevicePolicyManager
                                                                                .ACTION_ADD_DEVICE_ADMIN
                                                                )
                                                                .apply {
                                                                        putExtra(
                                                                                DevicePolicyManager
                                                                                        .EXTRA_DEVICE_ADMIN,
                                                                                adminComponent
                                                                        )
                                                                        putExtra(
                                                                                DevicePolicyManager
                                                                                        .EXTRA_ADD_EXPLANATION,
                                                                                "Shows a warning before Device Admin is disabled. During an active focus lock, the enabled Accessibility Service may also dismiss a clearly identified FocusGuard uninstall confirmation."
                                                                        )
                                                                }
                                                context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                ) { Text("Enable Device Admin Warning") }
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
                                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                                val presetsPerRow =
                                                        if (maxWidth < 370.dp ||
                                                                        androidx.compose.ui.platform
                                                                                .LocalDensity
                                                                                .current
                                                                                .fontScale > 1.25f
                                                        )
                                                                3
                                                        else presets.size
                                                Column(
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(6.dp)
                                                ) {
                                                        presets.chunked(presetsPerRow).forEach {
                                                                rowPresets ->
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .spacedBy(
                                                                                                6.dp
                                                                                        )
                                                                ) {
                                                                        rowPresets.forEach {
                                                                                (mins, label) ->
                                                                                val isSelected =
                                                                                        selectedMinutes ==
                                                                                                mins
                                                                                if (isSelected) {
                                                                                        Button(
                                                                                                onClick = {
                                                                                                        selectedMinutes =
                                                                                                                mins
                                                                                                },
                                                                                                shape =
                                                                                                        RoundedCornerShape(
                                                                                                                8.dp
                                                                                                        ),
                                                                                                modifier =
                                                                                                        Modifier.weight(
                                                                                                                1f
                                                                                                        ),
                                                                                                contentPadding =
                                                                                                        PaddingValues(
                                                                                                                horizontal =
                                                                                                                        4.dp
                                                                                                        )
                                                                                        ) {
                                                                                                Text(
                                                                                                        label,
                                                                                                        fontSize =
                                                                                                                12.sp
                                                                                                )
                                                                                        }
                                                                                } else {
                                                                                        OutlinedButton(
                                                                                                onClick = {
                                                                                                        selectedMinutes =
                                                                                                                mins
                                                                                                },
                                                                                                shape =
                                                                                                        RoundedCornerShape(
                                                                                                                8.dp
                                                                                                        ),
                                                                                                modifier =
                                                                                                        Modifier.weight(
                                                                                                                1f
                                                                                                        ),
                                                                                                contentPadding =
                                                                                                        PaddingValues(
                                                                                                                horizontal =
                                                                                                                        4.dp
                                                                                                        )
                                                                                        ) {
                                                                                                Text(
                                                                                                        label,
                                                                                                        fontSize =
                                                                                                                12.sp
                                                                                                )
                                                                                        }
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                                onClick = {
                                                        requestConfirmation(
                                                                "Engage Strict Lock?",
                                                                "Settings and protection changes will remain locked for ${selectedMinutes} minutes before cooldown can begin.",
                                                                "Engage lock"
                                                        ) {
                                                                FocusPrefs.engageStrictLock(
                                                                        context,
                                                                        selectedMinutes
                                                                )
                                                                onLockStateChanged()
                                                        }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                        ) {
                                                Text(
                                                        "Engage Strict Lock",
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                }
                                FocusPrefs.LOCK_ENGAGED -> {
                                        val configuredMins =
                                                FocusPrefs.getConfiguredCooldownMinutes(context)
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
                                        ) { Text("Start Unlock Cooldown (${configuredMins}m)") }
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
                                        ) { Text("Cancel Unlock & Re-Lock Immediately") }
                                }
                        }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Educational Committee of Naseerpur (ECN) Panel
                SectionPanel {
                        Text(
                                text = "Educational Committee of Naseerpur",
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                                text = "ECN INITIATIVE · COMMUNITY & STUDENT WELFARE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentEmerald,
                                letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text =
                                        "Developed under the Educational Committee of Naseerpur (ECN) to help students and youth reclaim deep focus, guard their time from algorithmic distractions, and build disciplined study habits—100% free and ad-free.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ContactChip(
                                                label = "YouTube Channel",
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                // Replace with ECN's exact YouTube channel
                                                // handle/URL if needed
                                                uriHandler.openUri(
                                                        "https://www.youtube.com/@ECNaseerpur"
                                                )
                                        }
                                        ContactChip(
                                                label = "FocusGuard Website",
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                uriHandler.openUri(
                                                        "https://quasimkhan.github.io/FocusGuard/"
                                                )
                                        }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ContactChip(
                                                label = "Share FocusGuard",
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                val shareIntent =
                                                        Intent(Intent.ACTION_SEND).apply {
                                                                type = "text/plain"
                                                                putExtra(
                                                                        Intent.EXTRA_SUBJECT,
                                                                        "FocusGuard — Educational Committee of Naseerpur (ECN)"
                                                                )
                                                                putExtra(
                                                                        Intent.EXTRA_TEXT,
                                                                        "Stay focused and block YouTube Shorts, Reels & distractions with FocusGuard (by Educational Committee of Naseerpur):\nhttps://quasimkhan.github.io/FocusGuard"
                                                                )
                                                        }
                                                try {
                                                        context.startActivity(
                                                                Intent.createChooser(
                                                                        shareIntent,
                                                                        "Share FocusGuard"
                                                                )
                                                        )
                                                } catch (_: Exception) {}
                                        }
                                }
                        }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Developer, Updates & Contact Panel
                SectionPanel {
                        Text("Developer & Releases", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val stackButtons =
                                        maxWidth < 380.dp ||
                                                androidx.compose.ui.platform.LocalDensity.current
                                                        .fontScale > 1.25f
                                if (stackButtons) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
                                                        onClick = { reportIssue() },
                                                        modifier = Modifier.fillMaxWidth()
                                                ) { Text("Report a bug") }
                                                OutlinedButton(
                                                        onClick = { checkForUpdates() },
                                                        modifier = Modifier.fillMaxWidth()
                                                ) {
                                                        Text(
                                                                if (isCheckingUpdate)
                                                                        "Checking for updates…"
                                                                else "Check for updates"
                                                        )
                                                }
                                        }
                                } else {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                        onClick = { reportIssue() },
                                                        modifier = Modifier.weight(1f)
                                                ) { Text("Report bug") }
                                                OutlinedButton(
                                                        onClick = { checkForUpdates() },
                                                        modifier = Modifier.weight(1f)
                                                ) {
                                                        Text(
                                                                if (isCheckingUpdate) "Checking…"
                                                                else "Check updates"
                                                        )
                                                }
                                        }
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
                                color = TextMuted,
                                modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ContactChip(
                                                label = "GitHub",
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                uriHandler.openUri(
                                                        "https://github.com/quasimkhan/FocusGuard"
                                                )
                                        }
                                        ContactChip(
                                                label = "Email",
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                val mail =
                                                        Intent(
                                                                Intent.ACTION_SENDTO,
                                                                Uri.parse(
                                                                        "mailto:quasimdotdev@gmail.com"
                                                                )
                                                        )
                                                try {
                                                        context.startActivity(mail)
                                                } catch (_: Exception) {}
                                        }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ContactChip(label = "X", modifier = Modifier.weight(1f)) {
                                                uriHandler.openUri("https://x.com/quasimdotdev")
                                        }
                                        ContactChip(
                                                label = "LinkedIn",
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                uriHandler.openUri(
                                                        "https://linkedin.com/in/quasimkhan"
                                                )
                                        }
                                }
                        }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                        text = "FocusGuard v$installedVersion",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted.copy(alpha = 0.7f)
                )
                Text(
                        text = "Developed by Quasim.™",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted.copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.height(18.dp))
        }
}

@Composable
fun ContactChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
        Box(
                modifier =
                        modifier.clip(RoundedCornerShape(8.dp))
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Text(
                        label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                )
                Text(
                        text = if (active) "ACTIVE" else "OFF",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) AccentEmerald else MaterialTheme.colorScheme.error
                )
        }
}

private fun formatMinuteOfDay(minutes: Int): String =
        String.format("%02d:%02d", (minutes / 60).coerceIn(0, 23), (minutes % 60).coerceIn(0, 59))

fun formatDuration(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val hours = totalSec / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        return String.format("%02d:%02d:%02d", hours, mins, secs)
}
