package com.audiomack.model

enum class ReportContentReason(val key: String) {
    Violent("violent-pornographic"),
    Broken("broken-stream"),
    Misleading("mislabeled"),
    Spam("spam-harassment"),
    Infringement("infringement")
}
