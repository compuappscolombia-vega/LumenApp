package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.MutedStone
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.TealAtmos
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onSignedIn: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isBusy by remember { mutableStateOf(false) }
    var attemptedSilent by remember { mutableStateOf(false) }
    var silentJob by remember { mutableStateOf<Job?>(null) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!attemptedSilent) {
            attemptedSilent = true
            // Let the first SurfaceSyncGroup frame transaction commit before CredentialManager IPC
            withFrameNanos { }
            delay(180L)
            silentJob = launch {
                val user = authRepository.attemptAutoSignIn(context)
                if (user != null) {
                    onSignedIn()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0E0E14), ObsidianBg, Color(0xFF13110D))
                )
            )
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Image Banner with Cinema Noir gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cinema_studio_hero),
                    contentDescription = stringResource(R.string.cd_auth_hero),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    ObsidianBg.copy(alpha = 0.72f),
                                    ObsidianBg.copy(alpha = 0.96f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = AmberGold,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = null,
                                tint = ObsidianBg,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.studio_brand_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = IvoryPrimary
                        )
                        Text(
                            text = stringResource(R.string.studio_brand_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberGold
                        )
                    }
                }
            }

            // Copy & Feature Pill Summary
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.auth_headline),
                    style = MaterialTheme.typography.displaySmall,
                    color = IvoryPrimary
                )
                Text(
                    text = stringResource(R.string.auth_subhead),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedStone
                )
            }

            Surface(
                color = StudioCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Acceso privado al estudio",
                            style = MaterialTheme.typography.titleMedium,
                            color = IvoryPrimary
                        )
                    }

                    if (!authError.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_error_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = authError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = stringResource(R.string.auth_troubleshooting_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (isBusy) return@Button
                            authError = null
                            isBusy = true
                            silentJob?.cancel()
                            silentJob = null
                            scope.launch {
                                val res = authRepository.signInInteractive(context)
                                isBusy = false
                                res.onSuccess {
                                    onSignedIn()
                                }.onFailure { err ->
                                    authError = err.localizedMessage
                                        ?: context.getString(R.string.auth_error_cancelled)
                                }
                            }
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = ObsidianBg,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(10.dp))
                            Text(
                                text = stringResource(R.string.auth_signing_in),
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.auth_google_cta),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = TealAtmos,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.auth_privacy_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedStone
                        )
                    }
                }
            }
        }
    }
}
