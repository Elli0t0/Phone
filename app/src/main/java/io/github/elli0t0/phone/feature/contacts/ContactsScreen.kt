package io.github.elli0t0.phone.feature.contacts

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.permissions.rememberContactsPermissionState
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.feature.contacts.ui.ContactDetailScreen
import io.github.elli0t0.phone.feature.contacts.ui.ContactEditScreen
import io.github.elli0t0.phone.feature.contacts.ui.ContactHistoryScreen
import io.github.elli0t0.phone.feature.contacts.ui.ContactListScreen
import io.github.elli0t0.phone.feature.contacts.ui.GroupsScreen
import kotlinx.coroutines.launch

private sealed interface ContactsNavDestination {
    data object List : ContactsNavDestination
    data class Detail(val contactId: Long, val lookupKey: String) : ContactsNavDestination
    data class History(val contactId: Long, val lookupKey: String) : ContactsNavDestination
    data object Create : ContactsNavDestination
    data class Edit(val contact: ContactDetail) : ContactsNavDestination
    data object Groups : ContactsNavDestination
}

/** Navigation depth: drives the inner-screen slide direction. */
private fun destinationDepth(destination: ContactsNavDestination): Int = when (destination) {
    ContactsNavDestination.List -> 0
    ContactsNavDestination.Groups -> 1
    is ContactsNavDestination.Detail -> 2
    is ContactsNavDestination.Create -> 2
    is ContactsNavDestination.History -> 3
    is ContactsNavDestination.Edit -> 3
}

@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onShowBottomBarChanged: (Boolean) -> Unit = {}
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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // The bottom bar shows only on the contact list, not on detail/edit/groups.
    LaunchedEffect(currentDestination) {
        onShowBottomBarChanged(currentDestination is ContactsNavDestination.List)
    }

    // Read here, not inside transitionSpec: the spec lambda is not a
    // @Composable context, so it cannot read a CompositionLocal.
    val animationOption = LocalAppSettings.current.animationOption

    // Show user message from listViewModel if any
    val userMessageRes = uiState.userMessageRes
    val messageText = userMessageRes?.let { stringResource(id = it) }
    LaunchedEffect(userMessageRes) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            listViewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    val direction = destinationDepth(targetState) - destinationDepth(initialState)
                    Motion.innerContentTransform(animationOption, direction)
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
                            currentDestination = ContactsNavDestination.Create
                        },
                        onFindDuplicates = listViewModel::findDuplicates,
                        onShowFavoritesHint = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.contacts_favorites_hint))
                            }
                        },
                        onShowDuplicates = listViewModel::showDuplicates,
                        onDismissDuplicatesCard = listViewModel::dismissDuplicatesCard,
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
                                currentDestination = ContactsNavDestination.Edit(contact)
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


                is ContactsNavDestination.Create -> {
                    val createViewModel: ContactEditViewModel = viewModel(
                        key = "create_contact",
                        factory = ContactEditViewModel.provideFactory(repository, null)
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
                        key = "edit_${dest.contact.id}",
                        factory = ContactEditViewModel.provideFactory(repository, dest.contact)
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
                                currentDestination = ContactsNavDestination.Detail(savedId, dest.contact.lookupKey)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_contact_updated))
                                }
                            }
                        },
                        onCancelClick = {
                            currentDestination = ContactsNavDestination.Detail(dest.contact.id, dest.contact.lookupKey)
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
        }
    }
}
