package com.audiomack.data.autocompletion

class EmailAutocompletionEngine : EmailAutocompletionInterface {

    override fun getCompletionForPrefix(prefix: String, ignoreCase: Boolean): String {

        // Check that text field contains an @
        if (!prefix.contains("@")) {
            return ""
        }

        // Stop autocomplete if user types dot after domain
        val domainAndTLD = prefix.substring(prefix.indexOf("@"))
        if (domainAndTLD.contains(".")) {
            return ""
        }

        // Check that there aren't two @-signs
        val textComponents =
            prefix.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (textComponents.size > 2) {
            return ""
        }

        if (textComponents.size == 1 || textComponents.size == 2) {

            // If no domain is entered, use the first domain in the list
            if (textComponents.size == 1 && prefix.indexOf("@") == prefix.length - 1) {
                return emailDomains[0]
            }

            if (textComponents.size == 2) {

                var stringToLookFor = textComponents[1]
                if (ignoreCase) {
                    stringToLookFor = stringToLookFor.toLowerCase()
                }

                for (stringFromReference in emailDomains) {
                    var stringToCompare = stringFromReference
                    if (ignoreCase) {
                        stringToCompare = stringToCompare.toLowerCase()
                    }
                    if (stringToCompare.startsWith(stringToLookFor)) {
                        return stringFromReference.replace(stringToLookFor.toRegex(), "")
                    }
                }
            }
        }

        return ""
    }

    companion object {

        private val emailDomains = arrayOf(
            "gmail.com",
            "yahoo.com",
            "hotmail.com",
            "aol.com",
            "comcast.net",
            "me.com",
            "msn.com",
            "live.com",
            "sbcglobal.net",
            "ymail.com",
            "att.net",
            "mac.com",
            "cox.net",
            "verizon.net",
            "hotmail.co.uk",
            "bellsouth.net",
            "rocketmail.com",
            "aim.com",
            "yahoo.co.uk",
            "earthlink.net",
            "charter.net",
            "optonline.net",
            "shaw.ca",
            "yahoo.ca",
            "googlemail.com",
            "mail.com",
            "qq.com",
            "btinternet.com",
            "mail.ru",
            "live.co.uk",
            "naver.com",
            "rogers.com",
            "juno.com",
            "yahoo.com.tw",
            "live.ca",
            "walla.com",
            "163.com",
            "roadrunner.com",
            "telus.net",
            "embarqmail.com",
            "hotmail.fr",
            "pacbell.net",
            "sky.com",
            "sympatico.ca",
            "cfl.rr.com",
            "tampabay.rr.com",
            "q.com",
            "yahoo.co.in",
            "yahoo.fr",
            "hotmail.ca",
            "windstream.net",
            "hotmail.it",
            "web.de",
            "asu.edu",
            "gmx.de",
            "gmx.com",
            "insightbb.com",
            "netscape.net",
            "icloud.com",
            "frontier.com",
            "126.com",
            "hanmail.net",
            "suddenlink.net",
            "netzero.net",
            "mindspring.com",
            "ail.com",
            "windowslive.com",
            "netzero.com",
            "yahoo.com.hk",
            "yandex.ru",
            "mchsi.com",
            "cableone.net",
            "yahoo.com.cn",
            "yahoo.es",
            "yahoo.com.br",
            "cornell.edu",
            "ucla.edu",
            "us.army.mil",
            "excite.com",
            "ntlworld.com",
            "usc.edu",
            "nate.com",
            "outlook.com",
            "nc.rr.com",
            "prodigy.net",
            "wi.rr.com",
            "videotron.ca",
            "yahoo.it",
            "yahoo.com.au",
            "umich.edu",
            "ameritech.net",
            "libero.it",
            "yahoo.de",
            "rochester.rr.com",
            "cs.com",
            "frontiernet.net",
            "swbell.net",
            "msu.edu",
            "ptd.net",
            "proxymail.facebook.com",
            "hotmail.es",
            "austin.rr.com",
            "nyu.edu",
            "sina.com",
            "centurytel.net",
            "usa.net",
            "nycap.rr.com",
            "uci.edu",
            "hotmail.de",
            "yahoo.com.sg",
            "email.arizona.edu",
            "yahoo.com.mx",
            "ufl.edu",
            "bigpond.com",
            "unlv.nevada.edu",
            "yahoo.cn",
            "ca.rr.com",
            "google.com",
            "yahoo.co.id",
            "inbox.com",
            "fuse.net",
            "hawaii.rr.com",
            "talktalk.net",
            "gmx.net",
            "walla.co.il",
            "ucdavis.edu",
            "carolina.rr.com",
            "comcast.com",
            "live.fr",
            "blueyonder.co.uk",
            "live.cn",
            "cogeco.ca",
            "abv.bg",
            "tds.net",
            "centurylink.net",
            "yahoo.com.vn",
            "uol.com.br",
            "osu.edu",
            "san.rr.com",
            "rcn.com",
            "umn.edu",
            "live.nl",
            "live.com.au",
            "tx.rr.com",
            "eircom.net",
            "sasktel.net",
            "post.harvard.edu",
            "snet.net",
            "wowway.com",
            "live.it",
            "hoteltonight.com",
            "att.com",
            "vt.edu",
            "rambler.ru",
            "temple.edu",
            "cinci.rr.com",
            "telenet.be",
            "skynet.be",
            "home.nl",
            "ziggo.nl",
            "planet.nl",
            "kpnmail.nl",
            "hetnet.nl",
            "upcmail.nl",
            "xs4all.nl",
            "casema.nl",
            "chello.nl",
            "kpnplanet.nl",
            "hotmail.nl",
            "telfort.nl",
            "online.nl",
            "zonnet.nl",
            "quicknet.nl",
            "solcon.nl",
            "tiscali.nl",
            "versatel.nl",
            "tele2.nl",
            "zeelandnet.nl"
        )
    }
}
