# croviera

```
the only tester for this app is my girlfriend. shoutout to her. i love you.
```

a local-first android music player with a terminal soul and an amoled problem. (i think)

croviera is what happens when someone looks at a music player and thinks
"what if theres a music player that... plays music i guess, and also what if it was held
together by hope and gradle". it plays music. it also plays with your patience.

# screenshot or sm?????
<img width="246" height="550" alt="Screenshot_2026-11-03-20-29-17-04_ced7f87d148198a9a5d73debfbcaaa47" src="https://github.com/user-attachments/assets/53c26346-7fc9-46ea-8e88-c07042ffa099" />

---

## the honest disclaimer

this is a horrible project. it is a vibecoded mess. i am aware of this.
i have made peace with this. (´･ω･`)

it started as a prototype, got out of hand, and is now a ~~full~~ app with a
rebrand, a version number that finally starts at 1.0, and a 1.1 that adds a
fullscreen player and even more customization. please do not look at the
commit history. please do look at the settings menu because i think its also a mess. (￣▽￣)ゞ

if something breaks, it is probably a feature. if it works, it is definitely
an accident. (╯°□°)╯︵ ┻━┻

---

## what it does

- plays local audio files from folders you choose
- amoled-black croviera theme with white text and a quietly dark red accent
- system24 and a pile of other themes are still there if you want them
- now playing card that opens a fullscreen, metro-style player when tapped
- fullscreen player with a big cover card, scrubbing, queue, lyrics, and favorite
- lyrics support with sidecar .lrc files and a metadata editor for assigning one
- playlists with rename, reorder, custom covers, and json import/export
- stats with song metadata instead of ugly file paths
- artist pages with song sorting by album, year, a-z, or length
- artist albums sort by year, a-z, or total length
- home screen widget with album art and playback controls
- full backup and restore for settings, stats, playlists, covers, fonts, and images

## customization (yes, all of it)

wallahi you can do this (more coming soon).

- move the top buttons to the top, bottom, left, or right
- rename every button and give it a nerd font glyph
- toggle nerd font icons instead of text
- set corner radius and border width
- add a colorable drop shadow under every panel, with offset and opacity controls
- give the custom theme a square replacement image that fills every panel
- pick a custom gradient for the custom theme
- custom background images belong to the saved custom theme, not every theme
- choose the now playing card title plus fullscreen queue and lyrics defaults
- crop, pan, and zoom profile pictures, banners, disc icons, backgrounds, and square images
- pick a tab transition: fade, cmatrix, slides, zoom, or none
- adjust font size, line height, row padding, cover size, and panel opacity
- install .ttf fonts from a file instead of pasting a url
- import stats and playlists from a file instead of pasting json

## the .lrc thing

croviera looks for lyrics next to the song, in the app lyrics folder, and in
mediastore. if it still cannot find them, you can assign a .lrc file in the
metadata editor. the app will even say:

```
You actually have an .lrc file? Apply one in the metadata editor!
```

yes, that exact sentence. with the space. (๑•̀ㅂ•́)و✧

## install

download the latest apk from the releases page:

```
https://github.com/emperornyctos/croviera/releases
```

install it, grant storage/audio permission, pick your music folders, and
pretend everything is fine. (・ω・)ノ

## build

you need android studio or a working command line android sdk.

```
git clone https://github.com/emperornyctos/croviera.git
cd croviera
./gradlew assembleRelease
```

the apk lands in `app/build/outputs/apk/release/`.

## license

mit. take it, change it, break it, fix it. just do not blame me. (´；ω；`)

```
please give me the bodypop vinyl i want it and it will be mine please
```
