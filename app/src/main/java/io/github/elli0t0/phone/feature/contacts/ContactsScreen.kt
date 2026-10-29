package io.github.elli0t0.phone.feature.contacts

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.LocalAppSnackbarHostState
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneBottomSheet
import io.github.elli0t0.phone.core.permissions.rememberContactsPermissionState
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.settings.model.Backdrop
import io.github.elli0t0.phone.feature.contacts.ui.ContactDetailScreen
import io.github.elli0t0.phone.feature.contacts.ui.ContactEditScreen
import io.github.elli0t0.phone.feature.contacts.ui.ContactHistoryScreen
import io.github.elli0t0.phone.feature.contacts.ui.ContactListScreen
import io.github.elli0t0.phone.feature.contacts.ui.GroupsScreen
import io.github.elli0t0.phone.feature.contacts.ui.ContactPickerScreen
import io.github.elli0t0.phone.navigation.CreateContactRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private sealed interface ContactsNavDestination {
    data object List : ContactsNavDestination
    data class Detail(val contactId: Long, val lookupKey: String) : ContactsNavDestination
    data class History(val contactId: Long, val lookupKey: String) : ContactsNavDestination
    data class Create(
        val initialPhone: String = "",
        val initialName: String = "",
        val initialEmail: String = ""
    ) : ContactsNavDestination
    data class Edit(
        val contact: ContactDetail,
        val appendPhone: String = "",
        val returnToList: Boolean = false
    ) : ContactsNavDestination
    data class PickContact(val number: String) : ContactsNavDestination
    data object Groups : ContactsNavDestination
}

/** Navigation depth: drives the inner-screen slide direction. */
private fun destinationDepth(destination: ContactsNavDestination): Int = when (destination) {
    ContactsNavDestination.List -> 0
    ContactsNavDestination.Groups -> 1
    is ContactsNavDestination.Detail -> 2
    is ContactsNavDestination.Create -> 2
    is ContactsNavDestination.PickContact -> 2
    is ContactsNavDestination.History -> 3
    is ContactsNavDestination.Edit -> 3
}

@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onShowBottomBarChanged: (Boolean) -> Unit = {},
    pendingCreateRequest: CreateContactRequest? = null,
    onPendingCreateRequestConsumed: () -> Unit = {},
    pendingAddNumber: String? = null,
    onPendingAddNumberConsumed: () -> Unit = {},
    pendingOpenContactId: Long? = null,
    onPendingOpenContactIdConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val repository = app.container.contactsRepository

    // Activity-scoped: the list data survives tab switches, so returning
    // to the tab shows the previous list instantly (no skeleton, no empty
    // state, no re-query). Detail/edit/groups VMs stay entry-scoped.
    val listViewModel: ContactsViewModel = viewModel(
        viewModelStoreOwner = context as ComponentActivity,
        factory = ContactsViewModel.provideFactory(
            repository,
            app.container.favoritesRepository,
            app.container.settingsRepository
        )
    )
    val uiState by listViewModel.uiState.collectAsStateWithLifecycle()
    val permissionState = rememberContactsPermissionState()

    var currentDestination by remember { mutableStateOf<ContactsNavDestination>(ContactsNavDestination.List) }
    // Classic opens the editor as a destination; Clean as a sheet
    // over the list (or detail) underneath.
    var editSheet by remember { mutableStateOf<ContactsNavDestination?>(null) }
    val useSheets = LocalAppSettings.current.effectiveStyle.sheets
    // App-level snackbar host (MainActivity): one host above the
    // floating bar for every screen, no per-screen host under the bar.
    val snackbarHostState = LocalAppSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    // A create request (keypad / Recents) opens
    // the editor prefilled with the number (and name/email when the
    // source provided them), then clears the pending value.
    LaunchedEffect(pendingCreateRequest) {
        if (pendingCreateRequest != null) {
            val create = ContactsNavDestination.Create(
                pendingCreateRequest.phone,
                pendingCreateRequest.name,
                pendingCreateRequest.email
            )
            if (useSheets) editSheet = create else currentDestination = create
            onPendingCreateRequestConsumed()
        }
    }

    // "Add to a contact" opens the in-app picker for the number.
    LaunchedEffect(pendingAddNumber) {
        if (pendingAddNumber != null) {
            listViewModel.clearSearchQuery()
            currentDestination = ContactsNavDestination.PickContact(pendingAddNumber)
            onPendingAddNumberConsumed()
        }
    }

    // A contact id from Recents opens that contact's detail, then clears
    // the pending value. The lookup key is empty here: the detail loads
    // by id and picks up the real key from the loaded contact.
    LaunchedEffect(pendingOpenContactId) {
        if (pendingOpenContactId != null) {
            currentDestination = ContactsNavDestination.Detail(pendingOpenContactId, "")
            onPendingOpenContactIdConsumed()
        }
    }

    // The bottom bar shows only on the contact list, not on detail/edit/groups.
    LaunchedEffect(currentDestination) {
        onShowBottomBarChanged(currentDestination is ContactsNavDestination.List)
    }

    // Read here, not inside transitionSpec: the spec lambda is not a
    // @Composable context, so it cannot read a CompositionLocal.
    val animationOption = LocalAppSettings.current.animationOption
    val transitionStyle = LocalAppSettings.current.effectiveStyle.transitions

    // Show user message from listViewModel if any
    val userMessageRes = uiState.userMessageRes
    val messageText = userMessageRes?.let { stringResource(id = it) }
    LaunchedEffect(userMessageRes) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            listViewModel.clearMessage()
        }
    }

    // Transparent when the ambient backdrop is live so it shows
    // through the gaps, exactly like Recents (the list screen itself
    // is already transparent; this outer Scaffold was the opaque
    // layer). Otherwise the theme background, as before. List cards
    // stay opaque either way.
    val backdropVisible = LocalAppSettings.current.let {
        it.backdrop != Backdrop.OFF && !it.pureBlackDarkMode
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (backdropVisible) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.background
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    val direction = destinationDepth(targetState) - destinationDepth(initialState)
                    Motion.innerContentTransform(animationOption, transitionStyle, direction)
                },
                label = "contacts_destination"
            ) { dest ->
                when (dest) {
                is ContactsNavDestination.List -> {
                    ContactListScreen(
                        uiState = uiState,
                        permissionState = permissionState,
                        onSearchQueryChanged = listViewModel::onSearchQueryChanged,
                        onClearSearch = listViewModel::clearSearchQuery,
                        onNavigateToSettings = onNavigateToSettings,
                        onContactClick = { contact ->
                            currentDestination = ContactsNavDestination.Detail(contact.id, contact.lookupKey)
                        },
                        onAddContactClick = {
                            if (useSheets) {
                                editSheet = ContactsNavDestination.Create()
                            } else {
                                currentDestination = ContactsNavDestination.Create()
                            }
                        },
                        onFindDuplicates = listViewModel::findDuplicates,
                        onShowFavoritesHint = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.contacts_favorites_hint))
                            }
                        },
                        onShowDuplicates = listViewModel::showDuplicates,
                        onDismissDuplicatesCard = listViewModel::dismissDuplicatesCard,
                        onDismissFavoritesHint = listViewModel::dismissFavoritesHint,
                        onGroupsClick = { currentDestination = ContactsNavDestination.Groups },
                        onBackFromDuplicates = listViewModel::hideDuplicates,
                        onMergeRequest = listViewModel::requestMerge,
                        onDismissMerge = listViewModel::dismissMergeDialog,
                        onConfirmMerge = { keepId -> listViewModel.confirmMerge(keepId) }
                    )
                }

                is ContactsNavDestination.Groups -> {
                    val groupsViewModel: GroupsViewModel = viewModel(
                        factory = GroupsViewModel.provideFactory(repository)
                    )
                    GroupsScreen(
                        viewModel = groupsViewModel,
                        onBack = { currentDestination = ContactsNavDestination.List },
                        onMemberClick = { contact ->
                            currentDestination = ContactsNavDestination.Detail(contact.id, contact.lookupKey)
                        }
                    )
                }

                is ContactsNavDestination.Detail -> {
                    val detailViewModel: ContactDetailViewModel = viewModel(
                        key = "detail_${dest.contactId}",
                        factory = ContactDetailViewModel.provideFactory(
                            repository,
                            app.container.favoritesRepository,
                            dest.contactId,
                            dest.lookupKey,
                            app.container.contactSimRepository,
                            app.container.callLogRepository
                        )
                    )
                    val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()

                    ContactDetailScreen(
                        uiState = detailUiState,
                        onBackClick = { currentDestination = ContactsNavDestination.List },
                        onEditClick = {
                            detailUiState.contact?.let { contact ->
                                if (useSheets) {
                                    editSheet = ContactsNavDestination.Edit(contact)
                                } else {
                                    currentDestination = ContactsNavDestination.Edit(contact)
                                }
                            }
                        },
                        onFavoriteToggle = { detailViewModel.toggleFavorite() },
                        onDeleteRequest = { detailViewModel.setDeleteDialogVisible(true) },
                        onConfirmDelete = {
                            detailViewModel.deleteContact {
                                currentDestination = ContactsNavDestination.List
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_contact_deleted))
                                }
                            }
                        },
                        onDismissDeleteDialog = { detailViewModel.setDeleteDialogVisible(false) },
                        onPhotoPicked = detailViewModel::onPhotoPickedForPreview,
                        onConfirmSavePhoto = detailViewModel::onConfirmSavePhoto,
                        onDiscardPendingPhoto = detailViewModel::onDiscardPendingPhoto,
                        onRemovePhoto = detailViewModel::onRemovePhoto,
                        onPhotoPreviewRequest = { detailViewModel.setPhotoPreviewVisible(true) },
                        onDismissPhotoPreview = { detailViewModel.setPhotoPreviewVisible(false) },
                        onPhotoOptionsRequest = { detailViewModel.setPhotoOptionsDialogVisible(true) },
                        onDismissPhotoOptions = { detailViewModel.setPhotoOptionsDialogVisible(false) },
                        onRingtoneSelected = detailViewModel::onRingtoneSelected,
                        contactSimRepository = app.container.contactSimRepository,
                        onCallingSimSelected = detailViewModel::setCallingSim,
                        onShowAllHistoryClick = {
                            currentDestination = ContactsNavDestination.History(dest.contactId, dest.lookupKey)
                        }
                    )
                }

                is ContactsNavDestination.History -> {
                    val historyViewModel: ContactHistoryViewModel = viewModel(
                        key = "history_${dest.contactId}",
                        factory = ContactHistoryViewModel.provideFactory(
                            repository,
                            app.container.callLogRepository,
                            dest.contactId,
                            dest.lookupKey
                        )
                    )
                    ContactHistoryScreen(
                        viewModel = historyViewModel,
                        onBack = {
                            currentDestination = ContactsNavDestination.Detail(dest.contactId, dest.lookupKey)
                        }
                    )
                }


                is ContactsNavDestination.PickContact -> {
                    ContactPickerScreen(
                        uiState = uiState,
                        onSearchQueryChanged = listViewModel::onSearchQueryChanged,
                        onClearSearch = listViewModel::clearSearchQuery,
                        onContactClick = { contact ->
                            coroutineScope.launch {
                                val detail = repository.getContactDetail(contact.lookupKey, contact.id).first()
                                listViewModel.clearSearchQuery()
                                if (detail != null) {
                                    val edit = ContactsNavDestination.Edit(
                                        detail,
                                        appendPhone = dest.number,
                                        returnToList = true
                                    )
                                    if (useSheets) editSheet = edit else currentDestination = edit
                                }
                            }
                        },
                        onBack = {
                            listViewModel.clearSearchQuery()
                            currentDestination = ContactsNavDestination.List
                        }
                    )
                }

                // Classic only: the editor as a destination (Clean
                // opens it as a sheet instead, see editSheet below).
                is ContactsNavDestination.Create -> {
                    val createViewModel: ContactEditViewModel = viewModel(
                        // Keyed by the prefilled values: a fresh editor
                        // for each request, never a stale previous input.
                        key = "create_contact_${dest.initialPhone}_${dest.initialName.hashCode()}_${dest.initialEmail.hashCode()}",
                        factory = ContactEditViewModel.provideFactory(
                            repository,
                            null,
                            dest.initialPhone,
                            dest.initialName,
                            dest.initialEmail
                        )
                    )
                    val createUiState by createViewModel.uiState.collectAsStateWithLifecycle()

                    ContactEditScreen(
                        uiState = createUiState,
                        onNameChanged = createViewModel::onNameChanged,
                        onOrganizationChanged = createViewModel::onOrganizationChanged,
                        onPhoneNumberChanged = createViewModel::onPhoneNumberChanged,
                        onPhoneTypeChanged = createViewModel::onPhoneTypeChanged,
                        onAddPhone = createViewModel::addPhoneRow,
                        onRemovePhone = createViewModel::removePhoneRow,
                        onEmailAddressChanged = createViewModel::onEmailAddressChanged,
                        onEmailTypeChanged = createViewModel::onEmailTypeChanged,
                        onAddEmail = createViewModel::addEmailRow,
                        onRemoveEmail = createViewModel::removeEmailRow,
                        onSaveClick = {
                            createViewModel.saveContact { newId ->
                                currentDestination = ContactsNavDestination.Detail(newId, "")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_contact_created))
                                }
                            }
                        },
                        onCancelClick = { currentDestination = ContactsNavDestination.List },
                        onPhotoPicked = createViewModel::onPhotoPickedForPreview,
                        onConfirmSaveCroppedPhoto = createViewModel::onConfirmSaveCroppedPhoto,
                        onDiscardPendingCropPhoto = createViewModel::onDiscardPendingCropPhoto,
                        onRemovePhoto = createViewModel::onRemovePhoto,
                        onPhotoPreviewRequest = { createViewModel.setPhotoPreviewVisible(true) },
                        onDismissPhotoPreview = { createViewModel.setPhotoPreviewVisible(false) },
                        onPhotoOptionsRequest = { createViewModel.setPhotoOptionsDialogVisible(true) },
                        onDismissPhotoOptions = { createViewModel.setPhotoOptionsDialogVisible(false) },
                        onRingtoneSelected = createViewModel::onRingtoneSelected
                    )
                }
                is ContactsNavDestination.Edit -> {
                    val editViewModel: ContactEditViewModel = viewModel(
                        key = "edit_${dest.contact.id}_${dest.appendPhone}",
                        factory = ContactEditViewModel.provideFactory(repository, dest.contact, dest.appendPhone)
                    )
                    val editUiState by editViewModel.uiState.collectAsStateWithLifecycle()

                    ContactEditScreen(
                        uiState = editUiState,
                        onNameChanged = editViewModel::onNameChanged,
                        onOrganizationChanged = editViewModel::onOrganizationChanged,
                        onPhoneNumberChanged = editViewModel::onPhoneNumberChanged,
                        onPhoneTypeChanged = editViewModel::onPhoneTypeChanged,
                        onAddPhone = editViewModel::addPhoneRow,
                        onRemovePhone = editViewModel::removePhoneRow,
                        onEmailAddressChanged = editViewModel::onEmailAddressChanged,
                        onEmailTypeChanged = editViewModel::onEmailTypeChanged,
                        onAddEmail = editViewModel::addEmailRow,
                        onRemoveEmail = editViewModel::removeEmailRow,
                        onSaveClick = {
                            editViewModel.saveContact { savedId ->
                                if (dest.returnToList) {
                                    currentDestination = ContactsNavDestination.List
                                } else {
                                    currentDestination = ContactsNavDestination.Detail(
                                        dest.contact.id,
                                        dest.contact.lookupKey
                                    )
                                }
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_contact_updated))
                                }
                            }
                        },
                        onCancelClick = {
                            if (dest.returnToList) {
                                currentDestination = ContactsNavDestination.List
                            } else {
                                currentDestination = ContactsNavDestination.Detail(
                                    dest.contact.id,
                                    dest.contact.lookupKey
                                )
                            }
                        },
                        onPhotoPicked = editViewModel::onPhotoPickedForPreview,
                        onConfirmSaveCroppedPhoto = editViewModel::onConfirmSaveCroppedPhoto,
                        onDiscardPendingCropPhoto = editViewModel::onDiscardPendingCropPhoto,
                        onRemovePhoto = editViewModel::onRemovePhoto,
                        onPhotoPreviewRequest = { editViewModel.setPhotoPreviewVisible(true) },
                        onDismissPhotoPreview = { editViewModel.setPhotoPreviewVisible(false) },
                        onPhotoOptionsRequest = { editViewModel.setPhotoOptionsDialogVisible(true) },
                        onDismissPhotoOptions = { editViewModel.setPhotoOptionsDialogVisible(false) },
                        onRingtoneSelected = editViewModel::onRingtoneSelected
                    )
                }

                }
            }
            // Create/edit open as a bottom sheet over the list (or
            // detail) underneath, instead of replacing it.
            editSheet?.let { sheet ->
                // Drag-dismiss behaves like Cancel: an edit started
                // from the picker returns to the list, otherwise the
                // sheet just closes over the detail underneath.
                val onSheetCancel: () -> Unit = {
                    editSheet = null
                    if (sheet is ContactsNavDestination.Edit && sheet.returnToList) {
                        currentDestination = ContactsNavDestination.List
                    }
                }
                PhoneBottomSheet(onDismissRequest = onSheetCancel) {
                    when (sheet) {
                        is ContactsNavDestination.Create -> {
                            val createViewModel: ContactEditViewModel = viewModel(
                                // Keyed by the prefilled values: a fresh editor
                                // for each request, never a stale previous input.
                                key = "create_contact_${sheet.initialPhone}_${sheet.initialName.hashCode()}_${sheet.initialEmail.hashCode()}",
                                factory = ContactEditViewModel.provideFactory(
                                    repository,
                                    null,
                                    sheet.initialPhone,
                                    sheet.initialName,
                                    sheet.initialEmail
                                )
                            )
                            val createUiState by createViewModel.uiState.collectAsStateWithLifecycle()

                            ContactEditScreen(
                                uiState = createUiState,
                                onNameChanged = createViewModel::onNameChanged,
                                onOrganizationChanged = createViewModel::onOrganizationChanged,
                                onPhoneNumberChanged = createViewModel::onPhoneNumberChanged,
                                onPhoneTypeChanged = createViewModel::onPhoneTypeChanged,
                                onAddPhone = createViewModel::addPhoneRow,
                                onRemovePhone = createViewModel::removePhoneRow,
                                onEmailAddressChanged = createViewModel::onEmailAddressChanged,
                                onEmailTypeChanged = createViewModel::onEmailTypeChanged,
                                onAddEmail = createViewModel::addEmailRow,
                                onRemoveEmail = createViewModel::removeEmailRow,
                                onSaveClick = {
                                    createViewModel.saveContact { newId ->
                                        editSheet = null
                                        currentDestination = ContactsNavDestination.Detail(newId, "")
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.snackbar_contact_created))
                                        }
                                    }
                                },
                                onCancelClick = onSheetCancel,
                                onPhotoPicked = createViewModel::onPhotoPickedForPreview,
                                onConfirmSaveCroppedPhoto = createViewModel::onConfirmSaveCroppedPhoto,
                                onDiscardPendingCropPhoto = createViewModel::onDiscardPendingCropPhoto,
                                onRemovePhoto = createViewModel::onRemovePhoto,
                                onPhotoPreviewRequest = { createViewModel.setPhotoPreviewVisible(true) },
                                onDismissPhotoPreview = { createViewModel.setPhotoPreviewVisible(false) },
                                onPhotoOptionsRequest = { createViewModel.setPhotoOptionsDialogVisible(true) },
                                onDismissPhotoOptions = { createViewModel.setPhotoOptionsDialogVisible(false) },
                                onRingtoneSelected = createViewModel::onRingtoneSelected
                            )
                        }
                        is ContactsNavDestination.Edit -> {
                            val editViewModel: ContactEditViewModel = viewModel(
                                key = "edit_${sheet.contact.id}_${sheet.appendPhone}",
                                factory = ContactEditViewModel.provideFactory(repository, sheet.contact, sheet.appendPhone)
                            )
                            val editUiState by editViewModel.uiState.collectAsStateWithLifecycle()

                            ContactEditScreen(
                                uiState = editUiState,
                                onNameChanged = editViewModel::onNameChanged,
                                onOrganizationChanged = editViewModel::onOrganizationChanged,
                                onPhoneNumberChanged = editViewModel::onPhoneNumberChanged,
                                onPhoneTypeChanged = editViewModel::onPhoneTypeChanged,
                                onAddPhone = editViewModel::addPhoneRow,
                                onRemovePhone = editViewModel::removePhoneRow,
                                onEmailAddressChanged = editViewModel::onEmailAddressChanged,
                                onEmailTypeChanged = editViewModel::onEmailTypeChanged,
                                onAddEmail = editViewModel::addEmailRow,
                                onRemoveEmail = editViewModel::removeEmailRow,
                                onSaveClick = {
                                    editViewModel.saveContact { savedId ->
                                        editSheet = null
                                        if (sheet.returnToList) {
                                            currentDestination = ContactsNavDestination.List
                                        }
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.snackbar_contact_updated))
                                        }
                                    }
                                },
                                onCancelClick = onSheetCancel,
                                onPhotoPicked = editViewModel::onPhotoPickedForPreview,
                                onConfirmSaveCroppedPhoto = editViewModel::onConfirmSaveCroppedPhoto,
                                onDiscardPendingCropPhoto = editViewModel::onDiscardPendingCropPhoto,
                                onRemovePhoto = editViewModel::onRemovePhoto,
                                onPhotoPreviewRequest = { editViewModel.setPhotoPreviewVisible(true) },
                                onDismissPhotoPreview = { editViewModel.setPhotoPreviewVisible(false) },
                                onPhotoOptionsRequest = { editViewModel.setPhotoOptionsDialogVisible(true) },
                                onDismissPhotoOptions = { editViewModel.setPhotoOptionsDialogVisible(false) },
                                onRingtoneSelected = editViewModel::onRingtoneSelected
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
