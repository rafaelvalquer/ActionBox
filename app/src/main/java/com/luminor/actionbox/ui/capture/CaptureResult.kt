package com.luminor.actionbox.ui.capture

import com.luminor.actionbox.R
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.actionTypeColor
import com.luminor.actionbox.ui.designsystem.components.ActionBadge
import com.luminor.actionbox.ui.designsystem.components.ActionButton
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CaptureResult(
    viewModel: CaptureViewModel,
    action: DetectedAction,
    editorVisible: Boolean,
    onToggleEditor: () -> Unit,
    onReset: () -> Unit
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    val context = LocalContext.current
    val color = actionTypeColor(action.type.name)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.saveDetected(context)
        else viewModel.showMessage(textResources.getString(R.string.text_permita_notificacoes_para_criar_acoes_com_aviso))
    }

    fun save() {
        val requiresPermission = action.type == ActionType.REMINDER || action.reminderMinutes != null
        if (requiresPermission && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else viewModel.saveDetected(context)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.medium, color = color.copy(alpha = 0.12f)) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(ActionBoxIcons.forType(action.type.name), contentDescription = null, tint = color)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(action.type.label, style = MaterialTheme.typography.labelLarge, color = color)
                Text(action.title, style = MaterialTheme.typography.titleLarge)
                action.scheduledAt?.let {
                    Text(
                        it.format(DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm", Locale.forLanguageTag("pt-BR"))).replaceFirstChar { ch -> ch.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onReset) { Icon(ActionBoxIcons.Close, contentDescription = textResources.getString(R.string.text_limpar)) }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ActionBadge(textResources.getString(R.string.text_confianca , action.confidenceLabel.lowercase()), color)
            androidx.compose.material3.TextButton(onClick = onToggleEditor) {
                Icon(ActionBoxIcons.Tune, contentDescription = null, modifier = Modifier.size(17.dp))
                Text(if (editorVisible) textResources.getString(R.string.text_ocultar_detalhes) else textResources.getString(R.string.text_editar_detalhes))
            }
        }

        AnimatedVisibility(visible = editorVisible) { CaptureEditor(viewModel, action) }

        ActionButton(
            text = when (action.type) {
                ActionType.LIST -> textResources.getString(R.string.text_criar_lista)
                ActionType.PROJECT -> textResources.getString(R.string.text_criar_projeto)
                ActionType.NOTE -> textResources.getString(R.string.text_salvar_nota)
                ActionType.EVENT -> textResources.getString(R.string.text_salvar_compromisso)
                ActionType.REMINDER -> textResources.getString(R.string.text_programar_lembrete)
                ActionType.REPLY -> textResources.getString(R.string.text_copiar_resposta)
                else -> textResources.getString(R.string.text_criar_acao)
            },
            onClick = {
                if (action.type == ActionType.REPLY) {
                    val option = viewModel.replyOptions(action.sourceText).firstOrNull()
                    if (option != null) viewModel.copyReply(context, option.text)
                } else save()
            }
        )

        if (action.type == ActionType.EVENT) {
            ActionButton(textResources.getString(R.string.text_salvar_e_adicionar_ao_calendario), onClick = { viewModel.saveDetectedAndOpenCalendar(context) }, primary = false)
        }
        if (action.type == ActionType.CONTACT) {
            ActionButton(textResources.getString(R.string.text_salvar_nos_contatos), onClick = { viewModel.insertContact(context, action.metadata ?: action.content) }, primary = false)
        }
    }
}
