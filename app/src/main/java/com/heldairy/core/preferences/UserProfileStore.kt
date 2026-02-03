package com.heldairy.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class UserProfile(
    val userName: String = "Alex",
    val avatarUri: String? = null
)

private val Context.userProfileDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile")

class UserProfileStore(private val context: Context) {
    
    private object PreferenceKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val AVATAR_URI = stringPreferencesKey("avatar_uri")
    }
    
    val profileFlow: Flow<UserProfile> = context.userProfileDataStore.data.map { preferences ->
        UserProfile(
            userName = preferences[PreferenceKeys.USER_NAME] ?: "Alex",
            avatarUri = preferences[PreferenceKeys.AVATAR_URI]
        )
    }
    
    suspend fun updateUserName(name: String) {
        context.userProfileDataStore.edit { preferences ->
            preferences[PreferenceKeys.USER_NAME] = name
        }
    }
    
    suspend fun updateAvatar(uri: String?) {
        context.userProfileDataStore.edit { preferences ->
            if (uri != null) {
                preferences[PreferenceKeys.AVATAR_URI] = uri
            } else {
                preferences.remove(PreferenceKeys.AVATAR_URI)
            }
        }
    }
}
