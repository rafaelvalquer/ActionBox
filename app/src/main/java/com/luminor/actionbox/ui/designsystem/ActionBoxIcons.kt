package com.luminor.actionbox.ui.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.luminor.actionbox.domain.ActionType

object ActionBoxIcons {
    val Home = Icons.Rounded.Home
    val Agenda = Icons.Rounded.CalendarMonth
    val Create = Icons.Rounded.AutoAwesome
    val Organize = Icons.Rounded.DashboardCustomize
    val Saved = Icons.Rounded.BookmarkBorder
    val Settings = Icons.Rounded.Settings
    val Paste = Icons.Rounded.ContentPaste
    val Arrow = Icons.AutoMirrored.Rounded.ArrowForward
    val More = Icons.Rounded.MoreVert
    val Back = Icons.AutoMirrored.Rounded.NavigateBefore
    val Next = Icons.AutoMirrored.Rounded.NavigateNext
    val Check = Icons.Rounded.Check
    val EmptyCheck = Icons.Rounded.RadioButtonUnchecked
    val Repeat = Icons.Rounded.Repeat
    val Time = Icons.Rounded.Schedule
    val Tune = Icons.Rounded.Tune
    val Chevron = Icons.Rounded.ChevronRight
    val Close = Icons.Rounded.Close
    val Archive = Icons.Rounded.Archive
    val Share = Icons.Rounded.Share
    val Copy = Icons.Rounded.ContentCopy
    val Delete = Icons.Rounded.Delete
    val Open = Icons.AutoMirrored.Rounded.OpenInNew
    val Fire = Icons.Rounded.LocalFireDepartment

    fun forType(type: String): ImageVector = when (type) {
        ActionType.REMINDER.name -> Icons.Rounded.Notifications
        ActionType.EVENT.name -> Icons.Rounded.Event
        ActionType.NOTE.name -> Icons.AutoMirrored.Rounded.StickyNote2
        ActionType.LIST.name -> Icons.Rounded.Checklist
        ActionType.PROJECT.name -> Icons.Rounded.Folder
        ActionType.READ_LATER.name -> Icons.Rounded.Bookmark
        else -> Icons.Rounded.TaskAlt
    }
}
