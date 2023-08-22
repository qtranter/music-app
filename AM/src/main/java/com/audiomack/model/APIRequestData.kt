package com.audiomack.model

import io.reactivex.Observable

class APIRequestData(var observable: Observable<APIResponseData>, val url: String?)
