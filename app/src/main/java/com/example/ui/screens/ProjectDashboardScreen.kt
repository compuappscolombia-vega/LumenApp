package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ProductionProject
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.SourceMaterialType
import com.example.data.util.ProjectTemplateCatalog
import com.example.data.util.StudioLogic
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDashboardScreen(
    currentUser: FirebaseUser?,
    projects: List<ProductionProject>,
    activeProjectId: String,
    searchQuery: String,
    statusBanner: String?,
    errorBanner: String?,
    isDarkMode: Boolean = true,
    onToggleDarkMode: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onSearchChange: (String) -> Unit,
    onSelectProject: (ProductionProject) -> Unit,
    onCreateProjectClick: () -> Unit,
    onDeleteProject: (String) -> Unit,
    onOpenLearning: () -> Unit,
    onOpenBlueprint: () -> Unit,
    onOpenMetrics: () -> Unit = {},
    onExportManualPdf: () -> Unit,
    onExportLibraryPdf: () -> Unit,
    onOpenAccount: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier,
    openErrorCount: Int = 0,
    onOpenFolderManager: (ProductionProject) -> Unit = {},
    onOpenScriptEditor: (ProductionProject) -> Unit = {},
    onOpenInteractiveManual: () -> Unit = onExportManualPdf,
    onOpenErrorCenter: () -> Unit = {}
) {
    var selectedSourceFilter by remember { mutableStateOf<SourceMaterialType?>(null) }
    var overflowExpanded by remember { mutableStateOf(false) }
    var projectPendingDelete by remember { mutableStateOf<ProductionProject?>(null) }

    val filteredProjects = remember(projects, searchQuery, selectedSourceFilter) {
        StudioLogic.filterProjects(projects, searchQuery).filter { project ->
            selectedSourceFilter == null || project.defaultSourceType == selectedSourceFilter
        }
    }

    val totalSequences = remember(projects) {
        projects.sumOf { maxOf(it.sequencesCount, it.sequences.size) }
    }
    val totalFinals = remember(projects) {
        projects.sumOf { proj ->
            proj.sequences.count { it.versionStatus == SequenceVersionStatus.FINAL }
        }
    }

    val colors = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.onSurface
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_lumenreel_logo),
                            contentDescription = "Logo LumenReel",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, colors.primary.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                                .testTag("dashboard_header_logo")
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.dashboard_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.studio_brand_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dashboard_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Brightness6,
                            contentDescription = if (isDarkMode) "Cambiar a Modo Claro" else "Cambiar a Modo Oscuro",
                            tint = colors.primary
                        )
                    }
                    IconButton(
                        onClick = onOpenCalendar,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dashboard_calendar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = "Calendario de producción",
                            tint = colors.primary
                        )
                    }
                    IconButton(
                        onClick = onOpenMetrics,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dashboard_open_metrics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Insights,
                            contentDescription = "Abrir analítica del estudio",
                            tint = colors.primary
                        )
                    }
                    IconButton(
                        onClick = onOpenAccount,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dashboard_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = stringResource(R.string.cd_account_menu),
                            tint = colors.onSurface
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { overflowExpanded = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_overflow_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Más acciones del estudio",
                                tint = colors.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.Brightness6, contentDescription = null) },
                                text = { Text(if (isDarkMode) "Cambiar a Modo Claro" else "Cambiar a Modo Oscuro") },
                                onClick = {
                                    overflowExpanded = false
                                    onToggleDarkMode()
                                },
                                modifier = Modifier.testTag("dashboard_theme_menu_item")
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                                text = { Text("Calendario de producción") },
                                onClick = {
                                    overflowExpanded = false
                                    onOpenCalendar()
                                },
                                modifier = Modifier.testTag("dashboard_calendar_menu_item")
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.Insights, contentDescription = null) },
                                text = { Text("Analítica del estudio") },
                                onClick = {
                                    overflowExpanded = false
                                    onOpenMetrics()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.Psychology, contentDescription = null) },
                                text = { Text(stringResource(R.string.action_learning)) },
                                onClick = {
                                    overflowExpanded = false
                                    onOpenLearning()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.FolderSpecial, contentDescription = null) },
                                text = { Text(stringResource(R.string.action_blueprint)) },
                                onClick = {
                                    overflowExpanded = false
                                    onOpenBlueprint()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.AutoStories, contentDescription = null) },
                                text = { Text("Manual interactivo y PDF") },
                                onClick = {
                                    overflowExpanded = false
                                    onOpenInteractiveManual()
                                },
                                modifier = Modifier.testTag("dashboard_manual_menu_item")
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Build,
                                        contentDescription = null,
                                        tint = if (openErrorCount > 0) colors.error else colors.onSurface
                                    )
                                },
                                text = {
                                    Text(
                                        if (openErrorCount > 0) {
                                            "Errores y reparación ($openErrorCount)"
                                        } else {
                                            "Registro de errores y reparación"
                                        }
                                    )
                                },
                                onClick = {
                                    overflowExpanded = false
                                    onOpenErrorCenter()
                                },
                                modifier = Modifier.testTag("dashboard_errors_menu_item")
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null) },
                                text = { Text(stringResource(R.string.action_library_pdf)) },
                                onClick = {
                                    overflowExpanded = false
                                    onExportLibraryPdf()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateProjectClick,
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("dashboard_new_project_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.action_new_project),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.surfaceVariant.copy(alpha = 0.45f), colors.background, colors.background)
                    )
                )
                .padding(innerPadding)
        ) {
            if (!errorBanner.isNullOrBlank()) {
                DashboardBannerBar(text = errorBanner, isError = true, onDismiss = onDismissBanner)
            } else if (!statusBanner.isNullOrBlank()) {
                DashboardBannerBar(text = statusBanner, isError = false, onDismiss = onDismissBanner)
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth >= 720.dp

                val telemetryHeader: @Composable () -> Unit = {
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Director: ${currentUser?.displayName ?: "Cuenta LumenReel"}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentUser?.email?.takeIf { it.isNotBlank() }
                                            ?.let { "$it · Perfil privado en Cloud Firestore" }
                                            ?: stringResource(R.string.dashboard_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DashboardStatPill(
                                    label = "Producciones",
                                    value = projects.size.toString(),
                                    accent = colors.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardStatPill(
                                    label = "Secuencias",
                                    value = totalSequences.toString(),
                                    accent = colors.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardStatPill(
                                    label = "Finales",
                                    value = totalFinals.toString(),
                                    accent = colors.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            val primaryTargetProject = remember(projects, activeProjectId) {
                                projects.firstOrNull { it.id == activeProjectId } ?: projects.firstOrNull()
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickStudioActionPill(
                                    icon = Icons.Rounded.Folder,
                                    label = "Carpetas",
                                    accent = colors.primary,
                                    onClick = {
                                        if (primaryTargetProject != null) onOpenFolderManager(primaryTargetProject)
                                        else onCreateProjectClick()
                                    },
                                    tag = "dashboard_quick_folders_button"
                                )
                                QuickStudioActionPill(
                                    icon = Icons.Rounded.EditNote,
                                    label = "Editor Guion IA",
                                    accent = colors.secondary,
                                    onClick = {
                                        if (primaryTargetProject != null) onOpenScriptEditor(primaryTargetProject)
                                        else onCreateProjectClick()
                                    },
                                    tag = "dashboard_quick_script_button"
                                )
                                QuickStudioActionPill(
                                    icon = Icons.Rounded.AutoStories,
                                    label = "Manual y PDF",
                                    accent = colors.primary,
                                    onClick = onOpenInteractiveManual,
                                    tag = "dashboard_quick_manual_button"
                                )
                                QuickStudioActionPill(
                                    icon = Icons.Rounded.Build,
                                    label = if (openErrorCount > 0) "Errores ($openErrorCount)" else "Errores / Reparar",
                                    accent = if (openErrorCount > 0) colors.error else colors.onSurfaceVariant,
                                    onClick = onOpenErrorCenter,
                                    tag = "dashboard_quick_errors_button"
                                )
                            }
                        }
                    }
                }

                val filterBar: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_projects_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                focusedContainerColor = colors.surface,
                                unfocusedContainerColor = colors.surface,
                                focusedTextColor = colors.onSurface,
                                unfocusedTextColor = colors.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_search_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DashboardFilterChip(
                                label = "Todas",
                                selected = selectedSourceFilter == null,
                                onClick = { selectedSourceFilter = null },
                                tag = "dashboard_filter_all"
                            )
                            SourceMaterialType.entries.forEach { source ->
                                DashboardFilterChip(
                                    label = when (source) {
                                        SourceMaterialType.BOOK -> "Libro"
                                        SourceMaterialType.AUDIOBOOK -> "Audiolibro"
                                        SourceMaterialType.STORY -> "Historia"
                                    },
                                    selected = selectedSourceFilter == source,
                                    onClick = {
                                        selectedSourceFilter = if (selectedSourceFilter == source) null else source
                                    },
                                    tag = "dashboard_filter_${source.wireValue}"
                                )
                            }
                        }
                    }
                }

                if (filteredProjects.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        telemetryHeader()
                        filterBar()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = colors.surface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .widthIn(max = 460.dp)
                                    .border(1.dp, colors.outline, RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Movie,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Text(
                                        text = if (projects.isEmpty()) {
                                            stringResource(R.string.empty_projects_title)
                                        } else {
                                            "Sin coincidencias para tu filtro"
                                        },
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.onSurface
                                    )
                                    Text(
                                        text = if (projects.isEmpty()) {
                                            stringResource(R.string.empty_projects_body)
                                        } else {
                                            "Prueba limpiando la búsqueda o crea una nueva producción cinematográfica."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = onCreateProjectClick,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.primary,
                                            contentColor = colors.onPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("dashboard_empty_create_button")
                                    ) {
                                        Text(
                                            text = if (projects.isEmpty()) {
                                                stringResource(R.string.action_create_first_project)
                                            } else {
                                                stringResource(R.string.action_new_project)
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (isWide) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("dashboard_projects_list"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 88.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) { telemetryHeader() }
                        item(span = { GridItemSpan(maxLineSpan) }) { filterBar() }
                        items(filteredProjects, key = { it.id }) { proj ->
                            ProjectDashboardCard(
                                project = proj,
                                isSelected = proj.id == activeProjectId,
                                onSelect = { onSelectProject(proj) },
                                onManageFolders = { onOpenFolderManager(proj) },
                                onOpenScriptEditor = { onOpenScriptEditor(proj) },
                                onRequestDelete = { projectPendingDelete = proj }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("dashboard_projects_list"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "telemetry_header") { telemetryHeader() }
                        item(key = "filter_bar") { filterBar() }
                        items(filteredProjects, key = { it.id }) { proj ->
                            ProjectDashboardCard(
                                project = proj,
                                isSelected = proj.id == activeProjectId,
                                onSelect = { onSelectProject(proj) },
                                onManageFolders = { onOpenFolderManager(proj) },
                                onOpenScriptEditor = { onOpenScriptEditor(proj) },
                                onRequestDelete = { projectPendingDelete = proj }
                            )
                        }
                    }
                }
            }
        }
    }

    projectPendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { projectPendingDelete = null },
            containerColor = colors.surface,
            titleContentColor = colors.onSurface,
            textContentColor = colors.onSurfaceVariant,
            title = { Text("Eliminar producción") },
            text = {
                Text("¿Deseas eliminar «${pending.title}» y sus secuencias asociadas de tu estudio privado?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(pending.id)
                        projectPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
                    modifier = Modifier.testTag("confirm_delete_project_button")
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectPendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel), color = colors.onSurfaceVariant)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectDashboardCard(
    project: ProductionProject,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onManageFolders: () -> Unit,
    onOpenScriptEditor: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val stats = remember(project.sequences, project.sequencesCount) {
        StudioLogic.computeProjectStats(project)
    }
    val displayedSeqCount = maxOf(project.sequencesCount, stats.sequenceCount)
    val finalsCount = remember(project.sequences) {
        project.sequences.count { it.versionStatus == SequenceVersionStatus.FINAL }
    }

    val templateMeta = remember(project.templateId) {
        ProjectTemplateCatalog.findById(project.templateId)
    }
    val folders = remember(project.productionFolders, project.templateId) {
        project.productionFolders.ifEmpty { ProjectTemplateCatalog.defaultFoldersFor(project.templateId) }
    }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) colors.primary.copy(alpha = 0.8f) else colors.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .testTag("dashboard_project_card_${project.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = colors.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = StudioLogic.getSourceTypeLabel(project.defaultSourceType),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = colors.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.border(1.dp, colors.outline, RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = "${templateMeta.name} · ${folders.size} carpetas",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("project_folders_badge_${project.id}")
                            )
                        }
                        if (isSelected) {
                            Surface(
                                color = colors.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ACTIVA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = project.genreStyle,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onRequestDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dashboard_delete_project_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Eliminar ${project.title}",
                        tint = colors.onSurfaceVariant
                    )
                }
            }

            if (project.continuityBible.isNotBlank()) {
                Text(
                    text = project.continuityBible,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Biblia de continuidad lista para definir reglas visuales, personajes y tono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (folders.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    folders.forEach { folder ->
                        Surface(
                            color = colors.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                                .clickable(onClick = onManageFolders)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = colors.primaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onManageFolders)
                        .testTag("project_manage_folders_${project.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(5.dp))
                        Text(
                            text = "Carpetas (${folders.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
                Surface(
                    color = colors.secondaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenScriptEditor)
                        .testTag("project_open_script_${project.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(5.dp))
                        Text(
                            text = "Guion IA",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Surface(
                color = colors.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildString {
                            append("$displayedSeqCount secuencias")
                            if (finalsCount > 0) append(" · $finalsCount finales")
                            if (stats.shotCount > 0) append(" · ${stats.shotCount} planos")
                            if (stats.estimatedMinutes > 0.0) append(" · ${stats.estimatedMinutes} min")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Abrir estudio",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, colors.outline, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun QuickStudioActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    tag: String
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DashboardFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.primaryContainer,
            selectedLabelColor = colors.onPrimaryContainer,
            containerColor = colors.surface,
            labelColor = colors.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.outline,
            selectedBorderColor = colors.primary.copy(alpha = 0.65f)
        ),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun DashboardBannerBar(
    text: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (isError) colors.errorContainer else colors.secondaryContainer,
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
                color = if (isError) colors.onErrorContainer else colors.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.action_close),
                    color = if (isError) colors.onErrorContainer else colors.onSecondaryContainer,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
