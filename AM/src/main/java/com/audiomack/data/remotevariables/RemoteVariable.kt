package com.audiomack.data.remotevariables

open class RemoteVariable(val key: String, val default: Any)

data class BooleanRemoteVariable(val booleanKey: String, val booleanDefault: Boolean) : RemoteVariable(booleanKey, booleanDefault)
data class LongRemoteVariable(val longKey: String, val longDefault: Long) : RemoteVariable(longKey, longDefault)
data class StringRemoteVariable(val stringKey: String, val stringDefault: String) : RemoteVariable(stringKey, stringDefault)
