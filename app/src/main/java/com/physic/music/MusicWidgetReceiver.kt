package com.physic.music

/**
 * Compatibility shim. Existing home-screen widgets are bound to this old
 * component name, so keeping it lets them survive the Croviera update.
 * New widgets use the same component and render the Croviera implementation.
 */
class MusicWidgetReceiver : com.croviera.music.MusicWidgetReceiver()
