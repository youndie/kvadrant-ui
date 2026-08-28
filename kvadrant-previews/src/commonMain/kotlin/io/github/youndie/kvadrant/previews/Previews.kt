package io.github.youndie.kvadrant.previews

/**
 * The order the site reads: foundation, then the controls, then the lists and pickers, then the
 * tiles, then navigation, then motion. It is the order somebody evaluating a component library
 * looks in, and it is *declaration* order rather than alphabetical so that related previews stay
 * next to each other.
 */
internal fun buildPreviews(): List<KvadrantPreview> =
    foundationPreviews() +
        controlPreviews() +
        listPreviews() +
        tilePreviews() +
        navigationPreviews() +
        motionPreviews()
