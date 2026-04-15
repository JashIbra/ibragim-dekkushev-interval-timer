package com.ibragimdekkushev.intervaltimer.presentation.load

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ibragimdekkushev.intervaltimer.R
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.IntervalTimerTheme

@Composable
fun LoadTimerScreen(
    onTimerLoaded: (timerId: Int) -> Unit,
    viewModel: LoadTimerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is LoadTimerUiState.Success) {
            onTimerLoaded((uiState as LoadTimerUiState.Success).timerId)
            viewModel.resetToIdle()
        }
    }

    LoadTimerContent(
        uiState = uiState,
        onLoad = viewModel::loadTimer,
        onInputChanged = { if (uiState is LoadTimerUiState.Error) viewModel.resetToIdle() },
    )
}

@Composable
private fun LoadTimerContent(
    uiState: LoadTimerUiState,
    onLoad: (String) -> Unit,
    onInputChanged: () -> Unit,
) {
    var idInput by rememberSaveable { mutableStateOf("68") }
    val isLoading = uiState is LoadTimerUiState.Loading
    val isError = uiState is LoadTimerUiState.Error

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.load_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.load_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.load_id_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = idInput,
                    onValueChange = { newValue ->
                        idInput = newValue
                        onInputChanged()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    enabled = !isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onLoad(idInput) }),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            if (isError) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onLoad(idInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.load_loading))
                } else {
                    Text(stringResource(if (isError) R.string.load_retry else R.string.load_button))
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadTimerIdlePreview() {
    IntervalTimerTheme {
        LoadTimerContent(
            uiState = LoadTimerUiState.Idle,
            onLoad = {},
            onInputChanged = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadTimerErrorPreview() {
    IntervalTimerTheme {
        LoadTimerContent(
            uiState = LoadTimerUiState.Error("Тренировка не найдена. Проверьте ID."),
            onLoad = {},
            onInputChanged = {},
        )
    }
}
