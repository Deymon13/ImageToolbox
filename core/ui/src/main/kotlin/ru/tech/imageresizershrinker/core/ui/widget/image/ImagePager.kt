/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2024 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package ru.tech.imageresizershrinker.core.ui.widget.image

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.toggleScale
import net.engawapg.lib.zoomable.zoomable
import ru.tech.imageresizershrinker.core.resources.R
import ru.tech.imageresizershrinker.core.resources.icons.BrokenImageVariant
import ru.tech.imageresizershrinker.core.resources.icons.EditAlt
import ru.tech.imageresizershrinker.core.ui.theme.White
import ru.tech.imageresizershrinker.core.ui.theme.takeColorFromScheme
import ru.tech.imageresizershrinker.core.ui.utils.animation.PageCloseTransition
import ru.tech.imageresizershrinker.core.ui.utils.animation.PageOpenTransition
import ru.tech.imageresizershrinker.core.ui.utils.helper.ContextUtils.getFilename
import ru.tech.imageresizershrinker.core.ui.utils.navigation.Screen
import ru.tech.imageresizershrinker.core.ui.widget.buttons.EnhancedIconButton
import ru.tech.imageresizershrinker.core.ui.widget.other.EnhancedTopAppBar
import ru.tech.imageresizershrinker.core.ui.widget.other.EnhancedTopAppBarType
import ru.tech.imageresizershrinker.core.ui.widget.sheets.ProcessImagesPreferenceSheet
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePager(
    visible: Boolean,
    selectedUri: Uri?,
    uris: List<Uri>?,
    onNavigate: (Screen) -> Unit,
    onUriSelected: (Uri?) -> Unit,
    onShare: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = PageOpenTransition,
        exit = PageCloseTransition
    ) {
        val density = LocalDensity.current
        val screenHeight =
            LocalConfiguration.current.screenHeightDp.dp + WindowInsets.systemBars.asPaddingValues()
                .let { it.calculateTopPadding() + it.calculateBottomPadding() }
        val anchors = with(density) {
            DraggableAnchors {
                true at 0f
                false at -screenHeight.toPx()
            }
        }

        val draggableState = remember(anchors) {
            AnchoredDraggableState(
                initialValue = true,
                anchors = anchors,
                positionalThreshold = { distance: Float -> distance * 0.5f },
                velocityThreshold = { with(density) { 100.dp.toPx() } },
                snapAnimationSpec = spring(),
                decayAnimationSpec = splineBasedDecay(density)
            )
        }

        LaunchedEffect(draggableState.settledValue) {
            if (!draggableState.settledValue) {
                onDismiss()
                delay(600)
                draggableState.snapTo(true)
            }
        }

        var wantToEdit by rememberSaveable {
            mutableStateOf(false)
        }
        val state = rememberPagerState(
            initialPage = selectedUri?.let {
                uris?.indexOf(it)
            } ?: 0,
            pageCount = {
                uris?.size ?: 0
            }
        )
        LaunchedEffect(state.currentPage) {
            onUriSelected(
                uris?.getOrNull(state.currentPage)
            )
        }
        val progress = draggableState.progress(
            from = false,
            to = true
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f * progress)
                )
        ) {
            val imageErrorPages = remember {
                mutableStateListOf<Int>()
            }
            HorizontalPager(
                state = state,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 5,
                pageSpacing = 16.dp
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val zoomState = rememberZoomState(10f)
                    Picture(
                        showTransparencyChecker = false,
                        model = uris?.getOrNull(page),
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .systemBarsPadding()
                            .displayCutoutPadding()
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = -draggableState
                                        .requireOffset()
                                        .roundToInt(),
                                )
                            }
                            .anchoredDraggable(
                                state = draggableState,
                                enabled = zoomState.scale < 1.01f,
                                orientation = Orientation.Vertical,
                                reverseDirection = true
                            )
                            .zoomable(
                                zoomEnabled = !imageErrorPages.contains(page),
                                zoomState = zoomState,
                                onDoubleTap = {
                                    zoomState.toggleScale(
                                        targetScale = 5f,
                                        position = it
                                    )
                                }
                            ),
                        contentScale = ContentScale.Fit,
                        shape = RectangleShape,
                        onSuccess = {
                            imageErrorPages.remove(page)
                        },
                        onError = {
                            imageErrorPages.add(page)
                        },
                        error = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.background(
                                    takeColorFromScheme { isNightMode ->
                                        errorContainer.copy(
                                            if (isNightMode) 0.25f
                                            else 1f
                                        ).compositeOver(surface)
                                    }
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BrokenImageVariant,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(0.5f),
                                    tint = MaterialTheme.colorScheme.onErrorContainer.copy(0.8f)
                                )
                            }
                        }
                    )
                }
            }
            AnimatedVisibility(
                visible = draggableState.offset == 0f,
                modifier = Modifier.fillMaxWidth(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                EnhancedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
                    ),
                    type = EnhancedTopAppBarType.Center,
                    modifier = Modifier,
                    title = {
                        uris?.size?.takeIf { it > 1 }?.let {
                            Text(
                                text = "${state.currentPage + 1}/$it",
                                modifier = Modifier
                                    .padding(vertical = 4.dp, horizontal = 12.dp),
                                color = White
                            )
                        }
                    },
                    actions = {
                        AnimatedVisibility(
                            visible = !uris.isNullOrEmpty(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EnhancedIconButton(
                                    onClick = {
                                        selectedUri?.let { onShare(it) }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = stringResource(R.string.share),
                                        tint = White
                                    )
                                }
                                EnhancedIconButton(
                                    onClick = {
                                        wantToEdit = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EditAlt,
                                        contentDescription = stringResource(R.string.edit),
                                        tint = White
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        AnimatedVisibility(!uris.isNullOrEmpty()) {
                            EnhancedIconButton(
                                onClick = {
                                    onDismiss()
                                    onUriSelected(null)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.exit),
                                    tint = White
                                )
                            }
                        }
                    }
                )
            }
            val context = LocalContext.current
            val selectedUriFilename by remember(selectedUri) {
                derivedStateOf {
                    selectedUri?.let {
                        context.getFilename(it)
                    }
                }
            }

            AnimatedVisibility(
                visible = draggableState.offset == 0f && !selectedUriFilename.isNullOrEmpty(),
                modifier = Modifier.fillMaxWidth(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectedUriFilename?.let {
                        Text(
                            text = it,
                            modifier = Modifier
                                .animateContentSize()
                                .padding(top = 64.dp)
                                .align(Alignment.TopCenter)
                                .padding(8.dp)
                                .statusBarsPadding()
                                .background(
                                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        ProcessImagesPreferenceSheet(
            uris = listOfNotNull(selectedUri),
            visible = wantToEdit,
            onDismiss = {
                wantToEdit = it
            },
            onNavigate = { screen ->
                scope.launch {
                    wantToEdit = false
                    delay(200)
                    onNavigate(screen)
                }
            }
        )

        if (visible) {
            BackHandler {
                onDismiss()
                onUriSelected(null)
            }
        }
    }
}