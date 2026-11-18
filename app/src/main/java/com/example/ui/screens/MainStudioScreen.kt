package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.WorkspaceTab
import com.example.data.repository.LOCAL_GUEST_UID
import com.example.data.util.StudioLogic
import com.example.ui.components.AccountDialog
import com.example.ui.components.AgentBlueprintDialog
import com.example.ui.components.LearningMemoryDialog
import com.example.ui.components.NewProjectDialog
import com.example.ui.components.SourceIngestModalSheet
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.ErrorLightText
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.MutedStone
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDeepPanel
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TealAtmos
import com.example.ui.theme.TealContainer
import com.example.ui.theme.WarmSilver
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.StudioViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MainStudioScreen(
    currentUser: FirebaseUser?,
    authStatusMessage: String? = null,
    onClearAuthStatus: () -> Unit = {},
    onOptionalGoogleSignIn: () -> Unit = {},
    onSignOut: () -> Unit,
    onShareFile: (File, String) -> Unit
) {
    val activeUid = currentUser?.uid ?: LOCAL_GUEST_UID
    val vm: StudioViewModel = viewModel(
        key = activeUid,
        factory = StudioViewModel.provideFactory(activeUid)
    )
    val uiState by vm.projectsUiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = AmberGold)
                    Text("Cargando estudio cinematográfico…", style = MaterialTheme.typography.bodyMedium, color = WarmSilver)
                }
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = ErrorContainerDark,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.widthIn(max = 460.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Error de conexión con el estudio", style = MaterialTheme.typography.titleMedium, color = ErrorLightText)
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = IvoryPrimary)
                        if (currentUser != null) {
                            OutlinedButton(onClick = onSignOut) {
                                Text(stringResource(R.string.action_sign_out), color = IvoryPrimary)
                            }
                        }
                    }
                }
            }
        }
        is UiState.Success -> {
            StudioWorkspaceShell(
                currentUser = currentUser,
                activeUid = activeUid,
                authStatusMessage = authStatusMessage,
                onClearAuthStatus = onClearAuthStatus,
                onOptionalGoogleSignIn = onOptionalGoogleSignIn,
                ui = state.data,
                vm = vm,
                onSignOut = onSignOut,
                onShareFile = onShareFile
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioWorkspaceShell(
    currentUser: FirebaseUser?,
    activeUid: String,
    authStatusMessage: String?,
    onClearAuthStatus: () -> Unit,
    onOptionalGoogleSignIn: () -> Unit,
    ui: StudioUiState,
    vm: StudioViewModel,
    onSignOut: () -> Unit,
    onShareFile: (File, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showLearningDialog by remember { mutableStateOf(false) }
    var showBlueprintDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var overflowExpanded by remember { mutableStateOf(false) }
    var sequenceMenuExpanded by remember { mutableStateOf(false) }

    if (ui.showDashboard) {
        ProjectDashboardScreen(
            currentUser = currentUser,
            projects = ui.projects,
            activeProjectId = ui.activeProject?.id.orEmpty(),
            searchQuery = ui.searchQuery,
            statusBanner = ui.statusBanner,
            errorBanner = ui.errorBanner,
            onSearchChange = vm::updateSearchQuery,
            onSelectProject = { proj ->
                vm.selectProject(proj.id)
                vm.setShowDashboard(false)
            },
            onCreateProjectClick = { showNewProjectDialog = true },
            onDeleteProject = vm::deleteProject,
            onOpenLearning = { showLearningDialog = true },
            onOpenBlueprint = { showBlueprintDialog = true },
            onExportManualPdf = { vm.exportUserManualPdf(context, onShareFile) },
            onExportLibraryPdf = { vm.exportLibraryPdf(context, ui.projects, onShareFile) },
            onOpenAccount = { showAccountDialog = true },
            onDismissBanner = vm::clearBanners
        )
    } else {
        BackHandler {
            vm.setShowDashboard(true)
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
            val isWideTablet = maxWidth >= 840.dp

            val drawerContent: @Composable () -> Unit = {
                StudioDrawerContent(
                    projects = ui.projects,
                    activeProjectId = ui.activeProject?.id.orEmpty(),
                    activeSequenceId = ui.activeSequence?.id.orEmpty(),
                    searchQuery = ui.searchQuery,
                    onSearchChange = vm::updateSearchQuery,
                    onOpenDashboard = {
                        vm.setShowDashboard(true)
                        if (!isWideTablet) scope.launch { drawerState.close() }
                    },
                    onSelectProject = { pid ->
                        vm.selectProject(pid)
                        if (!isWideTablet) scope.launch { drawerState.close() }
                    },
                    onSelectSequence = { sid ->
                        vm.selectSequence(sid)
                        if (!isWideTablet) scope.launch { drawerState.close() }
                    },
                    onDeleteProject = vm::deleteProject,
                    onCreateProjectClick = {
                        showNewProjectDialog = true
                        if (!isWideTablet) scope.launch { drawerState.close() }
                    }
                )
            }

        val scaffoldBody: @Composable () -> Unit = {
            Scaffold(
                containerColor = ObsidianBg,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = StudioSurface,
                            titleContentColor = IvoryPrimary
                        ),
                        navigationIcon = {
                            if (!isWideTablet) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("open_drawer_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Menu,
                                        contentDescription = stringResource(R.string.cd_open_drawer),
                                        tint = IvoryPrimary
                                    )
                                }
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    text = ui.activeProject?.title ?: stringResource(R.string.studio_brand_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val seq = ui.activeSequence
                                if (seq != null) {
                                    Box {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .clickable { sequenceMenuExpanded = true }
                                                .testTag("sequence_selector_button")
                                        ) {
                                            Text(
                                                text = seq.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AmberGold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Surface(
                                                color = if (seq.versionStatus == SequenceVersionStatus.FINAL) TealContainer else AmberContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (seq.versionStatus == SequenceVersionStatus.FINAL) "FINAL" else "BORRADOR",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (seq.versionStatus == SequenceVersionStatus.FINAL) TealAtmos else AmberGold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        val sequences = ui.activeProject?.sequences.orEmpty()
                                        DropdownMenu(
                                            expanded = sequenceMenuExpanded,
                                            onDismissRequest = { sequenceMenuExpanded = false }
                                        ) {
                                            sequences.forEach { item ->
                                                DropdownMenuItem(
                                                    text = { Text("${item.title} (${item.versionStatus.wireValue.uppercase()})") },
                                                    onClick = {
                                                        vm.selectSequence(item.id)
                                                        sequenceMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { vm.setShowDashboard(true) },
                                modifier = Modifier.testTag("open_dashboard_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GridView,
                                    contentDescription = stringResource(R.string.action_open_dashboard),
                                    tint = IvoryPrimary
                                )
                            }
                            val activeProj = ui.activeProject
                            val activeSeq = ui.activeSequence
                            if (activeProj != null) {
                                IconButton(
                                    onClick = { vm.setShowIngestSheet(true) },
                                    modifier = Modifier.testTag("open_ingest_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = stringResource(R.string.cd_open_ingest),
                                        tint = AmberGold
                                    )
                                }
                            }
                            if (activeProj != null && activeSeq != null && activeSeq.versionStatus == SequenceVersionStatus.DRAFT) {
                                IconButton(
                                    onClick = { vm.approveFinalVersion(activeProj, activeSeq) },
                                    enabled = !ui.isFinalizing,
                                    modifier = Modifier.testTag("approve_final_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = stringResource(R.string.action_approve_final),
                                        tint = TealAtmos
                                    )
                                }
                            }
                            Box {
                                IconButton(
                                    onClick = { overflowExpanded = true },
                                    modifier = Modifier.testTag("studio_overflow_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = "Más acciones del estudio",
                                        tint = IvoryPrimary
                                    )
                                }
                                DropdownMenu(
                                    expanded = overflowExpanded,
                                    onDismissRequest = { overflowExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Rounded.Psychology, contentDescription = null) },
                                        text = { Text(stringResource(R.string.action_learning)) },
                                        onClick = {
                                            overflowExpanded = false
                                            showLearningDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Rounded.FolderSpecial, contentDescription = null) },
                                        text = { Text(stringResource(R.string.action_blueprint)) },
                                        onClick = {
                                            overflowExpanded = false
                                            showBlueprintDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Rounded.AutoStories, contentDescription = null) },
                                        text = { Text(stringResource(R.string.action_user_manual)) },
                                        onClick = {
                                            overflowExpanded = false
                                            vm.exportUserManualPdf(context, onShareFile)
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null) },
                                        text = { Text(stringResource(R.string.action_library_pdf)) },
                                        onClick = {
                                            overflowExpanded = false
                                            vm.exportLibraryPdf(context, ui.projects, onShareFile)
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null) },
                                        text = { Text(currentUser?.displayName ?: "Estudio Local") },
                                        onClick = {
                                            overflowExpanded = false
                                            showAccountDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isWideTablet && ui.activeProject != null) {
                        NavigationBar(
                            containerColor = StudioSurface,
                            modifier = Modifier
                                .border(1.dp, StudioBorder)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            STUDIO_TABS.forEach { tabMeta ->
                                val selected = ui.activeTab == tabMeta.tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { vm.selectTab(tabMeta.tab) },
                                    icon = { Icon(tabMeta.icon, contentDescription = stringResource(tabMeta.labelRes)) },
                                    label = {
                                        Text(
                                            text = stringResource(tabMeta.labelRes),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AmberGold,
                                        selectedTextColor = AmberGold,
                                        indicatorColor = AmberContainer,
                                        unselectedIconColor = MutedStone,
                                        unselectedTextColor = MutedStone
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tabMeta.tab.routeKey}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideTablet && ui.activeProject != null) {
                        NavigationRail(
                            containerColor = StudioSurface,
                            modifier = Modifier.fillMaxHeight().border(1.dp, StudioBorder)
                        ) {
                            STUDIO_TABS.forEach { tabMeta ->
                                NavigationRailItem(
                                    selected = ui.activeTab == tabMeta.tab,
                                    onClick = { vm.selectTab(tabMeta.tab) },
                                    icon = { Icon(tabMeta.icon, contentDescription = stringResource(tabMeta.labelRes)) },
                                    label = { Text(stringResource(tabMeta.labelRes), style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = AmberGold,
                                        selectedTextColor = AmberGold,
                                        indicatorColor = AmberContainer,
                                        unselectedIconColor = MutedStone,
                                        unselectedTextColor = MutedStone
                                    ),
                                    modifier = Modifier.testTag("rail_tab_${tabMeta.tab.routeKey}")
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // Status / Error Banner
                        if (!ui.errorBanner.isNullOrBlank()) {
                            BannerBar(
                                text = ui.errorBanner,
                                isError = true,
                                onDismiss = vm::clearBanners
                            )
                        } else if (!ui.statusBanner.isNullOrBlank()) {
                            BannerBar(
                                text = ui.statusBanner,
                                isError = false,
                                onDismiss = vm::clearBanners
                            )
                        }

                        val activeProject = ui.activeProject
                        val activeSequence = ui.activeSequence

                        if (activeProject == null) {
                            EmptyStudioState(
                                title = stringResource(R.string.empty_projects_title),
                                body = stringResource(R.string.empty_projects_body),
                                ctaLabel = stringResource(R.string.action_create_first_project),
                                onCtaClick = { showNewProjectDialog = true },
                                ctaTag = "empty_create_project_button"
                            )
                        } else if (ui.activeTab == WorkspaceTab.AGENT) {
                            AgentTabView(
                                project = activeProject,
                                activeSequence = activeSequence,
                                messages = ui.agentMessages,
                                selectedFocus = ui.activeAgentFocus,
                                draftMessage = ui.activeAgentDraftMessage,
                                isBusy = ui.isAgentBusy,
                                onFocusChange = { focus -> vm.updateAgentFocus(activeProject.id, focus) },
                                onDraftChange = { text -> vm.updateAgentDraftMessage(activeProject.id, text) },
                                onSendMessage = { focus, text ->
                                    vm.sendAgentMessage(
                                        project = activeProject,
                                        activeSequence = activeSequence,
                                        recentMessages = ui.agentMessages,
                                        lessons = ui.lessons,
                                        focus = focus,
                                        userText = text
                                    )
                                },
                                onClearHistory = { vm.clearAgentHistory(activeProject.id) }
                            )
                        } else if (activeSequence == null) {
                            EmptyStudioState(
                                title = stringResource(R.string.empty_sequences_title),
                                body = stringResource(R.string.empty_sequences_body),
                                ctaLabel = stringResource(R.string.ingest_title),
                                onCtaClick = { vm.setShowIngestSheet(true) },
                                ctaTag = "empty_open_ingest_button",
                                secondaryCtaLabel = "Crear borrador manual en Guion",
                                onSecondaryCtaClick = {
                                    val draft = ui.activeIngestDraft
                                    vm.createManualDraftSequence(
                                        project = activeProject,
                                        sequenceTitle = draft.sequenceTitle,
                                        sourceType = draft.sourceType,
                                        directorStyle = draft.directorStyle,
                                        aspectRatio = draft.aspectRatio,
                                        continuityBible = draft.continuityBible,
                                        sourceText = draft.sourceText,
                                        attachedFilename = draft.attachedFile?.filename,
                                        attachedMimeType = draft.attachedFile?.mimeType,
                                        attachedBytes = draft.attachedFile?.bytes
                                    )
                                },
                                secondaryCtaTag = "empty_create_manual_draft_button"
                            )
                        } else {
                            when (ui.activeTab) {
                                WorkspaceTab.VISION -> VisionTabView(
                                    project = activeProject,
                                    sequence = activeSequence,
                                    directorDraft = ui.activeDirectorDraft,
                                    selectedRating = ui.activeVisionRating,
                                    ratingNote = ui.activeVisionRatingNote,
                                    intelligence = ui.intelligence,
                                    onDirectorDraftChange = { transform ->
                                        vm.updateDirectorDraft(activeProject, activeSequence, transform)
                                    },
                                    onSaveDirectorDraft = {
                                        vm.saveDirectorDraftEdits(activeProject, activeSequence)
                                    },
                                    onRatingChange = { r -> vm.updateVisionRatingDraft(activeSequence.id, r) },
                                    onRatingNoteChange = { n -> vm.updateVisionNoteDraft(activeSequence.id, n) },
                                    onApplyEnhancement = { snippet -> vm.applyDirectorialEnhancement(activeProject, snippet) },
                                    onRateSequence = { rating, note -> vm.rateSequence(activeProject, activeSequence, rating, note) }
                                )
                                WorkspaceTab.SHOTS -> ShotsAndSoundTabView(
                                    sequence = activeSequence,
                                    selectedShotIndex = ui.activeSelectedShotIndex,
                                    use3DStage = ui.activeStage3DMode,
                                    onSelectShot = { idx -> vm.updateSelectedShotIndex(activeSequence.id, idx) },
                                    onToggle3DStage = { use3d -> vm.updateStage3DMode(activeSequence.id, use3d) }
                                )
                                WorkspaceTab.SCREENPLAY -> ScreenplayTabView(
                                    sequence = activeSequence,
                                    isExporting = ui.isExporting,
                                    onOpenIngest = { vm.setShowIngestSheet(true) },
                                    onSaveSceneDraft = { sceneNum, slug, tod, action, character, dialogue, editIdx ->
                                        vm.saveScreenplaySceneDraft(
                                            projectId = activeProject.id,
                                            sequence = activeSequence,
                                            sceneNumber = sceneNum,
                                            slugline = slug,
                                            timeOfDay = tod,
                                            actionText = action,
                                            dialogueCharacter = character,
                                            dialogueText = dialogue,
                                            editingIndex = editIdx
                                        )
                                    },
                                    onExportPdf = { vm.exportSequencePdf(context, activeProject, activeSequence, onShareFile) },
                                    onExportMarkdown = {
                                        vm.exportScreenplayText(context, activeProject.title, activeSequence, fountain = false, onReadyToShare = onShareFile)
                                    },
                                    onExportFountain = {
                                        vm.exportScreenplayText(context, activeProject.title, activeSequence, fountain = true, onReadyToShare = onShareFile)
                                    }
                                )
                                WorkspaceTab.STORYBOARD -> StoryboardTabView(
                                    sequence = activeSequence,
                                    generatingFrameId = ui.generatingFrameId,
                                    onGenerateImage = { frame ->
                                        vm.generateStoryboardImage(context, activeProject.id, activeSequence, frame)
                                    }
                                )
                                WorkspaceTab.AGENT -> Unit
                            }
                        }
                    }
                }
            }
        }

        if (isWideTablet) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        drawerContainerColor = StudioSurface,
                        modifier = Modifier.width(290.dp)
                    ) { drawerContent() }
                },
                content = scaffoldBody
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = StudioSurface,
                        modifier = Modifier.width(300.dp)
                    ) { drawerContent() }
                },
                content = scaffoldBody
            )
        }
        }
    }

    if (ui.showIngestSheet && ui.activeProject != null) {
        val activeProj = ui.activeProject
        val draft = ui.activeIngestDraft
        SourceIngestModalSheet(
            project = activeProj,
            draft = draft,
            isProducing = ui.isProducing,
            isAnalyzingAudio = ui.isAnalyzingAudio,
            latestAudioReport = ui.latestAudioReport,
            onDraftChange = { transform -> vm.updateIngestDraft(activeProj, transform) },
            onDismiss = { vm.setShowIngestSheet(false) },
            onSaveBible = { text -> vm.saveContinuityBible(activeProj.id, text) },
            onAnalyzeAudio = { file ->
                vm.analyzeAudioSource(activeProj.id, file.filename, file.mimeType, file.bytes, ui.lessons)
            },
            onProduceSequence = {
                vm.produceSequence(
                    project = activeProj,
                    lessons = ui.lessons,
                    sequenceTitle = draft.sequenceTitle,
                    sourceType = draft.sourceType,
                    directorStyle = draft.directorStyle,
                    aspectRatio = draft.aspectRatio,
                    thinkingDepth = draft.thinkingDepth,
                    continuityBible = draft.continuityBible,
                    sourceText = draft.sourceText,
                    attachedFilename = draft.attachedFile?.filename,
                    attachedMimeType = draft.attachedFile?.mimeType,
                    attachedBytes = draft.attachedFile?.bytes,
                    onDone = { vm.setShowIngestSheet(false) }
                )
            },
            onCreateManualDraft = {
                vm.createManualDraftSequence(
                    project = activeProj,
                    sequenceTitle = draft.sequenceTitle,
                    sourceType = draft.sourceType,
                    directorStyle = draft.directorStyle,
                    aspectRatio = draft.aspectRatio,
                    continuityBible = draft.continuityBible,
                    sourceText = draft.sourceText,
                    attachedFilename = draft.attachedFile?.filename,
                    attachedMimeType = draft.attachedFile?.mimeType,
                    attachedBytes = draft.attachedFile?.bytes,
                    onDone = { vm.setShowIngestSheet(false) }
                )
            }
        )
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { title, genreStyle, sourceType, bible ->
                vm.createProject(title, genreStyle, sourceType, bible)
                vm.setShowDashboard(false)
            }
        )
    }

    if (showLearningDialog) {
        LearningMemoryDialog(
            lessons = ui.lessons,
            activeProjectTitle = ui.activeProject?.title.orEmpty(),
            onDismiss = { showLearningDialog = false },
            onAddLesson = vm::addLearningLesson,
            onDeleteLesson = vm::deleteLearningLesson
        )
    }

    if (showBlueprintDialog) {
        AgentBlueprintDialog(onDismiss = { showBlueprintDialog = false })
    }

    if (showAccountDialog) {
        AccountDialog(
            user = currentUser,
            authStatusMessage = authStatusMessage,
            onClearAuthStatus = onClearAuthStatus,
            onOptionalGoogleSignIn = onOptionalGoogleSignIn,
            onDismiss = { showAccountDialog = false },
            onSignOut = onSignOut
        )
    }
}

@Composable
private fun StudioDrawerContent(
    projects: List<ProductionProject>,
    activeProjectId: String,
    activeSequenceId: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenDashboard: () -> Unit,
    onSelectProject: (String) -> Unit,
    onSelectSequence: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onCreateProjectClick: () -> Unit
) {
    val filtered = remember(projects, searchQuery) {
        StudioLogic.filterProjects(projects, searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Movie, contentDescription = null, tint = AmberGold)
                Text(stringResource(R.string.studio_brand_title), style = MaterialTheme.typography.titleMedium, color = IvoryPrimary)
            }
            IconButton(
                onClick = onOpenDashboard,
                modifier = Modifier.testTag("drawer_open_dashboard_button")
            ) {
                Icon(Icons.Rounded.GridView, contentDescription = stringResource(R.string.action_open_dashboard), tint = AmberGold)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(stringResource(R.string.search_projects_hint), style = MaterialTheme.typography.bodySmall, color = MutedStone) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MutedStone) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AmberGold,
                unfocusedBorderColor = StudioBorder,
                focusedTextColor = IvoryPrimary,
                unfocusedTextColor = IvoryPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("drawer_search_input")
        )

        Button(
            onClick = onCreateProjectClick,
            colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBg),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("drawer_new_project_button")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.action_new_project), fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { proj ->
                val isSelected = proj.id == activeProjectId
                Surface(
                    color = if (isSelected) AmberContainer else StudioDeepPanel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isSelected) AmberGold else StudioBorder, RoundedCornerShape(12.dp))
                        .clickable { onSelectProject(proj.id) }
                        .testTag("project_item_${proj.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = proj.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) AmberGold else IvoryPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onDeleteProject(proj.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar producción", tint = MutedStone, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(proj.genreStyle, style = MaterialTheme.typography.labelSmall, color = MutedStone, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isSelected && proj.sequences.isNotEmpty()) {
                            Column(
                                modifier = Modifier.padding(top = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                proj.sequences.forEach { seq ->
                                    val seqActive = seq.id == activeSequenceId
                                    Surface(
                                        color = if (seqActive) StudioSurfaceElevated else StudioCard,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectSequence(seq.id) }
                                    ) {
                                        Text(
                                            text = "${seq.title} · ${if (seq.versionStatus == SequenceVersionStatus.FINAL) "Final" else "Borrador"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (seqActive) TealAtmos else WarmSilver,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
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

@Composable
private fun BannerBar(
    text: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        color = if (isError) ErrorContainerDark else TealContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) ErrorLightText else IvoryPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar aviso", tint = IvoryPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyStudioState(
    title: String,
    body: String,
    ctaLabel: String,
    onCtaClick: () -> Unit,
    ctaTag: String,
    secondaryCtaLabel: String? = null,
    onSecondaryCtaClick: (() -> Unit)? = null,
    secondaryCtaTag: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = StudioCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .widthIn(max = 460.dp)
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.Movie, contentDescription = null, tint = AmberGold, modifier = Modifier.size(38.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = IvoryPrimary)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MutedStone)
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onCtaClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(ctaTag)
                ) {
                    Text(ctaLabel, fontWeight = FontWeight.Bold)
                }
                if (secondaryCtaLabel != null && onSecondaryCtaClick != null) {
                    OutlinedButton(
                        onClick = onSecondaryCtaClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .let { if (secondaryCtaTag != null) it.testTag(secondaryCtaTag) else it }
                    ) {
                        Text(secondaryCtaLabel, color = TealAtmos, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private data class TabMeta(
    val tab: WorkspaceTab,
    val labelRes: Int,
    val icon: ImageVector
)

private val STUDIO_TABS = listOf(
    TabMeta(WorkspaceTab.AGENT, R.string.nav_agent, Icons.Rounded.SmartToy),
    TabMeta(WorkspaceTab.VISION, R.string.nav_vision, Icons.Rounded.Visibility),
    TabMeta(WorkspaceTab.SHOTS, R.string.nav_shots, Icons.Rounded.Videocam),
    TabMeta(WorkspaceTab.SCREENPLAY, R.string.nav_screenplay, Icons.Rounded.Description),
    TabMeta(WorkspaceTab.STORYBOARD, R.string.nav_storyboard, Icons.Rounded.ViewCarousel)
)
