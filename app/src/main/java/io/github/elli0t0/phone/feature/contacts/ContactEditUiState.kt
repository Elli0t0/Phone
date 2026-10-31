package io.github.elli0t0.phone.feature.contacts

import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactEmail
import io.github.elli0t0.phone.data.contacts.model.ContactPhone

data class ContactEditUiState(
    val isNewContact: Boolean = true,
    val contactId: Long = 0L,
    val lookupKey: String = "",
    val displayName: String = "",
    val organization: String = "",
    val notes: String = "",
    val birthday: String = "",
    val address: String = "",
    val phones: List<ContactPhone> = listOf(ContactPhone(number = "")),
    val emails: List<ContactEmail> = emptyList(),
    val initialPhotoUri: String? = null,
    val stagedPhotoBytes: ByteArray? = null,
    val isPhotoRemoved: Boolean = false,
    val customRingtone: String? = null,
    val showPhotoOptionsDialog: Boolean = false,
    val showPhotoPreview: Boolean = false,
    val pendingCropPhotoBytes: ByteArray? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val validationErrorRes: Int? = null,
    val originalDetail: ContactDetail? = null
)
