package com.audiomack.model

import com.activeandroid.Model
import com.activeandroid.annotation.Column
import com.activeandroid.annotation.Column.ConflictAction.IGNORE
import com.activeandroid.annotation.Table

/**
 * Items found locally that should be hidden from the user.
 */
@Table(name = "local_exclusions")
data class LocalMediaExclusion @JvmOverloads constructor(
    @Column(name = "mediaId", unique = true, onUniqueConflict = IGNORE) var mediaId: Long = -1L
) : Model()
