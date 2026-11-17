package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderSpecial
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ProductionProject
import com.example.data.model.SequenceVersionStatus
import com.example.data.model.SourceMaterialType
import com.example.data.util.StudioLogic
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.MutedStone
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDeepPanel
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TealAtmos
import com.example.ui.theme.TealContainer
import com.example.ui.theme.WarmSilver
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDashboardScreen(
    currentUser: FirebaseUser,
    projects: List<ProductionProject>,
    activeProjectId: String,
    searchQuery: String,
    statusBanner: String?,
    errorBanner: String?,
    onSearchChange: (String) -> Unit,
    onSelectProject: (ProductionProject) -> Unit,
    onCreateProjectClick: () -> Unit,
    onDeleteProject: (String) -> Unit,
    onOpenLearning: () -> Unit,
    onOpenBlueprint: () -> Unit,
    onExportManualPdf: () -> Unit,
    onExportLibraryPdf: () -> Unit,
    onOpenAccount: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioSurface,
                    titleContentColor = IvoryPrimary
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = AmberGold,
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Movie,
                                    contentDescription = null,
                                    tint = ObsidianBg,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.dashboard_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = IvoryPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.studio_brand_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenAccount,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dashboard_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = stringResource(R.string.cd_account_menu),
                            tint = IvoryPrimary
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
                                text = { Text(stringResource(R.string.action_user_manual)) },
                                onClick = {
                                    overflowExpanded = false
                                    onExportManualPdf()
                                }
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
                containerColor = AmberGold,
                contentColor = ObsidianBg,
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
                        listOf(Color(0xFF0E0E13), ObsidianBg, Color(0xFF100E0B))
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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Studio Overview Telemetry Banner
                    Surface(
                        color = StudioCard,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
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
                                        text = "Director: ${currentUser.displayName ?: "LumenReel"}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = IvoryPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.dashboard_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedStone
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
                                    accent = AmberGold,
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardStatPill(
                                    label = "Secuencias",
                                    value = totalSequences.toString(),
                                    accent = TealAtmos,
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardStatPill(
                                    label = "Finales",
                                    value = totalFinals.toString(),
                                    accent = IvoryPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Search & Source Filter Row
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_projects_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedStone
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = MutedStone)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGold,
                            unfocusedBorderColor = StudioBorder,
                            focusedContainerColor = StudioCard,
                            unfocusedContainerColor = StudioCard,
                            focusedTextColor = IvoryPrimary,
                            unfocusedTextColor = IvoryPrimary
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

                    // Active Projects List / Grid or Empty State
                    if (filteredProjects.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
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
                                    Icon(
                                        imageVector = Icons.Rounded.Movie,
                                        contentDescription = null,
                                        tint = AmberGold,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Text(
                                        text = if (projects.isEmpty()) {
                                            stringResource(R.string.empty_projects_title)
                                        } else {
                                            "Sin coincidencias para tu filtro"
                                        },
                                        style = MaterialTheme.typography.titleLarge,
                                        color = IvoryPrimary
                                    )
                                    Text(
                                        text = if (projects.isEmpty()) {
                                            stringResource(R.string.empty_projects_body)
                                        } else {
                                            "Prueba limpiando la búsqueda o crea una nueva producción cinematográfica."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MutedStone
                                    )
                                    Button(
                                        onClick = onCreateProjectClick,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AmberGold,
                                            contentColor = ObsidianBg
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
                    } else if (isWide) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 88.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredProjects, key = { it.id }) { proj ->
                                ProjectDashboardCard(
                                    project = proj,
                                    isSelected = proj.id == activeProjectId,
                                    onSelect = { onSelectProject(proj) },
                                    onRequestDelete = { projectPendingDelete = proj }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredProjects, key = { it.id }) { proj ->
                                ProjectDashboardCard(
                                    project = proj,
                                    isSelected = proj.id == activeProjectId,
                                    onSelect = { onSelectProject(proj) },
                                    onRequestDelete = { projectPendingDelete = proj }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    projectPendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { projectPendingDelete = null },
            containerColor = StudioCard,
            titleContentColor = IvoryPrimary,
            textContentColor = WarmSilver,
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
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson, contentColor = IvoryPrimary),
                    modifier = Modifier.testTag("confirm_delete_project_button")
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectPendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel), color = MutedStone)
                }
            }
        )
    }
}

@Composable
private fun ProjectDashboardCard(
    project: ProductionProject,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val stats = remember(project.sequences, project.sequencesCount) {
        StudioLogic.computeProjectStats(project)
    }
    val displayedSeqCount = maxOf(project.sequencesCount, stats.sequenceCount)
    val finalsCount = remember(project.sequences) {
        project.sequences.count { it.versionStatus == SequenceVersionStatus.FINAL }
    }

    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) AmberGold.copy(alpha = 0.8f) else StudioBorder,
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = TealContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = StudioLogic.getSourceTypeLabel(project.defaultSourceType),
                                style = MaterialTheme.typography.labelSmall,
                                color = TealAtmos,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (isSelected) {
                            Surface(
                                color = AmberContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ACTIVA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = IvoryPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = project.genreStyle,
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberGold,
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
                        tint = MutedStone
                    )
                }
            }

            if (project.continuityBible.isNotBlank()) {
                Text(
                    text = project.continuityBible,
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmSilver,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Biblia de continuidad lista para definir reglas visuales, personajes y tono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedStone,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = StudioDeepPanel,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
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
                        color = WarmSilver,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Abrir estudio",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberGold,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = AmberGold,
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
    Surface(
        color = StudioDeepPanel,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MutedStone, maxLines = 1)
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
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AmberContainer,
            selectedLabelColor = AmberGold,
            containerColor = StudioCard,
            labelColor = WarmSilver
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = StudioBorder,
            selectedBorderColor = AmberGold.copy(alpha = 0.65f)
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
    Surface(
        color = if (isError) com.example.ui.theme.ErrorContainerDark else TealContainer,
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
                color = if (isError) com.example.ui.theme.ErrorLightText else IvoryPrimary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), color = IvoryPrimary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
