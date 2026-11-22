package com.example.data.repository

import com.example.data.model.AgentRoleFocus
import com.example.data.model.DirectorDraftEdits
import com.example.data.model.IngestDraftState
import com.example.data.model.ProductionSequence
import com.example.data.model.WorkspaceTab
import kotlinx.coroutines.flow.MutableStateFlow

class StudioSessionStateHolder {
    val selectedProjectId = MutableStateFlow("")
    val selectedSequenceId = MutableStateFlow("")
    val activeTab = MutableStateFlow(WorkspaceTab.AGENT)
    val searchQuery = MutableStateFlow("")
    val showDashboard = MutableStateFlow(true)
    val showMetricsDashboard = MutableStateFlow(false)
    val showIngestSheet = MutableStateFlow(false)

    val sequencesByProject = MutableStateFlow<Map<String, List<ProductionSequence>>>(emptyMap())
    val ingestDrafts = MutableStateFlow<Map<String, IngestDraftState>>(emptyMap())
    val directorDraftsBySeq = MutableStateFlow<Map<String, DirectorDraftEdits>>(emptyMap())
    val visionRatingsBySeq = MutableStateFlow<Map<String, Int>>(emptyMap())
    val visionNotesBySeq = MutableStateFlow<Map<String, String>>(emptyMap())
    val selectedShotIndexBySeq = MutableStateFlow<Map<String, Int>>(emptyMap())
    val stage3DModeBySeq = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val agentFocusByProject = MutableStateFlow<Map<String, AgentRoleFocus>>(emptyMap())
    val agentDraftByProject = MutableStateFlow<Map<String, String>>(emptyMap())
    val localFramePaths = MutableStateFlow<Map<String, String>>(emptyMap())

    fun reset() {
        selectedProjectId.value = ""
        selectedSequenceId.value = ""
        activeTab.value = WorkspaceTab.AGENT
        searchQuery.value = ""
        showDashboard.value = true
        showMetricsDashboard.value = false
        showIngestSheet.value = false
        sequencesByProject.value = emptyMap()
        ingestDrafts.value = emptyMap()
        directorDraftsBySeq.value = emptyMap()
        visionRatingsBySeq.value = emptyMap()
        visionNotesBySeq.value = emptyMap()
        selectedShotIndexBySeq.value = emptyMap()
        stage3DModeBySeq.value = emptyMap()
        agentFocusByProject.value = emptyMap()
        agentDraftByProject.value = emptyMap()
        localFramePaths.value = emptyMap()
    }
}
