This repository holds all of our open source android apps and libraries

Unless otherwise stated all projects and files are licensed under the GPLv3

/*
 * Copyright (c) 2014-2017 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

Orpheus

Orpheus is the son of Apollo from Greek mythology. When Orpheus was first created it was forked from
the Apollo music player. Since then it has been rewritten half a dozen times and is now an original
creation. Old versions of Orpheus can be found in another repo [link].

The project here is the fourth or fifth revision (hard to remember). It is written entirely in
kotlin. This version is a dramatic simplification of the Orpheus codebase and overall design.

Playback

The core model item is the MediaItem and accompanying MediaDescription. Orpheus leverages the new
MediaSession API introduced in api21. The most basic element of the MediaItem is the mediaId. The
mediaId is a json representation of a MediaRef. The UI communicates with the playback service via
the MediaController class, which is part of the MediaSession API. The UI sends the service the
mediaId and the service knows what to play based on the MediaRef it builds with it. This is a far
departure from past implementations that sent lists of longs or uris through IPC to the service.
The problem with sending lists is that very large lists would break the 1MB IPC limit and crash the
service. Another problem was managing the queue. Cross-process queue management turned out to be
very cumbersome. The MediaSession api doesn't even allow queue management, an attempt was made in
the past to add it via out-of-band methods, but this proved complex and error prone. The decision
was made to try and work entirely within the MediaSession API. Hence queue management has been
removed. Great effort has been made to make the playback service as simple and dumb as possible.
This means no queue management, we can play lists of MediaItems but cannot rearrange or skip around,
we only play the list in order, top to bottom. The primary benefit to this simplicity is there is
no need for a shuffle action or a repeat action. The queue will always loop through the list, to loop
a single song, simply pass a single item. To play on shuffle, simply shuffle the list before giving
it to us. But that is slightly misleading. Remember we aren't given a list, only a mediaId. Thus, we
introduce the concept of playback sessions. A playback session is akin to a playlist and will be
discussed later.

Playback Flow:

*Passed a mediaId via a PlaybackController.
    *Fetches the associated item or items.
        Sorting is unavailable at this time, but could be introduced as an additional field in
        the mediaId.
    If mediaId is a collection, the starting mediaId can be passed via the extras bundle.
    While fetching the list the player is in STATE_BUFFERING. We do not use STATE_CONNECTING
    because Bluetooth A2DP does not handle STATE_CONNECTING and will go into an error state,
    breaking playback on some car head units (That was a mightily unpleasant bug to track down)
    *Once the list is fetched we send the current item to the renderer



UI

Past version of Orpheus tried to impose the writer will onto the users, he had a specific idea for
how music should be presented and wrote Orpheus to abide by that vision. In practice this worked
very poorly, with many angry emails sent telling him so. This prompted a change. The conventional
gallery of artists, albums, genres, etc are gone in favor of a pure folder browser. Now the user
can arrange the music however they want. A second issue involves Android's hatred of the SDCard,
Orpheus tried to circumvent this hatred but it didn't work. Now we work within the bounds of the
official method for accessing files on the SDCard, the DocumentProvider.

