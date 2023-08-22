package com.audiomack.model

enum class LoginSignupSource(val stringValue: String) {

    AppLaunch("App Launch"),
    Favorite("Favorite"),
    AccountFollow("Account Follow"),
    AddToPlaylist("Add to Playlist"),
    MyLibrary("My Library"),
    Repost("Repost"),
    ExpiredSession("Expired Session"),
    OfflinePlaylist("Offline Playlist"),
    Support("Support"),
    Highlight("Highlight"),
    Premium("Premium"),
    Download("Download"),
    Comment("Comment"),
    ResetPassword("Reset Password"),
    ChangePassword("Change Password"),
    OfflineFilter("Offline Filter");
}
