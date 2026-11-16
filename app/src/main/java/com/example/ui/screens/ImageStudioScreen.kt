package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppStrings
import com.example.data.ai.StudioAspectRatio
import com.example.data.files.DocumentExtractor
import com.example.data.model.AiModels
import com.example.ui.components.PremiumCard
import com.example.ui.components.saveBitmapToGallery
import com.example.ui.components.shareBitmapImage
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.VioletPrimary
import com.example.viewmodel.ImageGenUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageStudioScreen(
    strings: AppStrings,
    uiState: ImageGenUiState,
    onGenerateOrEdit: (
        prompt: String,
        modelId: String,
        aspectRatio: StudioAspectRatio,
        inputBitmap: Bitmap?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(AiModels.imageModels.first()) }
    var selectedAspect by remember { mutableStateOf(StudioAspectRatio.SQUARE) }
    var referenceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                referenceBitmap = DocumentExtractor.decodeBitmapFromUri(context, uri)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth(), accentBorder = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Image, contentDescription = null, tint = CyanSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.imageStudioTitle, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.imageStudioSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiModels.imageModels.forEach { model ->
                            FilterChip(
                                selected = selectedModel == model,
                                onClick = { selectedModel = model },
                                label = { Text(model) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(strings.imageAspectLabel, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudioAspectRatio.entries.forEach { ratio ->
                            FilterChip(
                                selected = selectedAspect == ratio,
                                onClick = { selectedAspect = ratio },
                                label = { Text(ratio.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text(strings.imagePromptLabel) },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_studio_prompt")
                    )

                    referenceBitmap?.let { bmp ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = strings.imagePickRef,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, CyanSecondary, RoundedCornerShape(12.dp))
                            )
                            OutlinedButton(onClick = { referenceBitmap = null }) {
                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.imageClearRef)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.imagePickRef)
                        }

                        Button(
                            onClick = {
                                onGenerateOrEdit(prompt, selectedModel, selectedAspect, referenceBitmap)
                            },
                            enabled = uiState !is ImageGenUiState.Loading && prompt.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("image_studio_generate_btn")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (referenceBitmap != null) strings.imageEditBtn else strings.imageGenerateBtn)
                        }
                    }
                }
            }

            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    when (uiState) {
                        is ImageGenUiState.Idle -> {
                            Image(
                                painter = painterResource(id = R.drawable.img_studio_canvas),
                                contentDescription = strings.imageStudioTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            )
                        }
                        is ImageGenUiState.Loading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = CyanSecondary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = strings.imageGeneratingHint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyanSecondary
                                )
                            }
                        }
                        is ImageGenUiState.Success -> {
                            Image(
                                bitmap = uiState.bitmap.asImageBitmap(),
                                contentDescription = strings.imageStudioTitle,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, CyanSecondary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        saveBitmapToGallery(context, uiState.bitmap, strings.savedToGallery)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldSuccess,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.saveToGallery)
                                }
                                OutlinedButton(
                                    onClick = { shareBitmapImage(context, uiState.bitmap) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.share)
                                }
                            }
                        }
                        is ImageGenUiState.Error -> {
                            Surface(
                                color = RoseError.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.message,
                                    color = RoseError,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
