package com.bajajfinance.beaconconsent.ui

/**
 * DPDP-aligned consent notice. REVIEW WITH LEGAL & INFOSEC before production.
 * Structured to meet DPDP Act 2023 notice requirements: identified data,
 * specified purpose, retention, rights (incl. withdrawal), and grievance contact.
 */
object ConsentText {

    const val TITLE = "Your consent to detect your paired devices"

    const val INTRO =
        "Bajaj Finance would like your permission to recognise you when you visit a " +
        "participating partner outlet, so our team can offer you relevant, pre-approved " +
        "finance options faster. This is entirely optional and you can withdraw anytime."

    val WHAT_WE_COLLECT = listOf(
        "A one-way encoded (hashed) identifier of the Bluetooth peripherals you choose " +
            "to register — such as your smartwatch or earbuds.",
        "The type of each device (e.g. watch, earbuds) and a label you can recognise.",
        "The date and time you gave consent, and the consent version."
    )

    val WHAT_WE_DONT = listOf(
        "We do NOT read your phone's location.",
        "We do NOT access your contacts, messages, photos, or call logs.",
        "We do NOT track you outside participating partner outlets.",
        "We do NOT store the raw device address in readable form — only a hash."
    )

    val PURPOSE = listOf(
        "Identify you at a participating outlet via a Bajaj Finance detection unit.",
        "Send you personalized, relevant finance offers."
    )

    const val RETENTION =
        "Your registration is kept only while you remain opted in. If you withdraw " +
        "consent, your records are deleted from our servers and this app."

    val RIGHTS = listOf(
        "Withdraw consent at any time from this app — your data is then erased.",
        "Request access to or correction of your registered data.",
        "Contact Bajaj Finserv customer care with any questions."
    )

    const val CONTACT =
        "Bajaj Finserv Customer Care — wecare@bajajfinserv.in"

    const val CHECKBOX_LABEL =
        "I have read the above and I consent to Bajaj Finance detecting my selected " +
        "paired devices at partner outlets for personalized offers."
}
