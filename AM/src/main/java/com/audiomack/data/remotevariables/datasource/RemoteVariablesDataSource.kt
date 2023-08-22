package com.audiomack.data.remotevariables.datasource

import com.audiomack.data.remotevariables.BooleanRemoteVariable
import com.audiomack.data.remotevariables.LongRemoteVariable
import com.audiomack.data.remotevariables.RemoteVariable
import com.audiomack.data.remotevariables.StringRemoteVariable
import io.reactivex.Observable

interface RemoteVariablesDataSource {

    fun init(list: List<RemoteVariable>): Observable<Boolean>

    fun getBoolean(remoteVariable: BooleanRemoteVariable): Boolean

    fun getLong(remoteVariable: LongRemoteVariable): Long

    fun getString(remoteVariable: StringRemoteVariable): String
}
