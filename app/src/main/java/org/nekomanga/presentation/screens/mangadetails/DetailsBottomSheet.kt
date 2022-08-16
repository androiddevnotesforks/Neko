package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.external.ExternalLink
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MergeConstants
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.sheets.ArtworkSheet
import org.nekomanga.presentation.components.sheets.EditCategorySheet
import org.nekomanga.presentation.components.sheets.ExternalLinksSheet
import org.nekomanga.presentation.components.sheets.FilterChapterSheet
import org.nekomanga.presentation.components.sheets.MergeSheet
import org.nekomanga.presentation.components.sheets.TrackingDateSheet
import org.nekomanga.presentation.components.sheets.TrackingSearchSheet
import org.nekomanga.presentation.components.sheets.TrackingSheet
import org.nekomanga.presentation.screens.ThemeColorState
import java.text.DateFormat

/**
 * Sealed class that holds the types of bottom sheets the details screen can show
 */
sealed class DetailsBottomSheetScreen {
    class CategoriesSheet(
        val addingToLibrary: Boolean = false,
        val setCategories: (List<Category>) -> Unit,
        val addToLibraryClick: () -> Unit = {},
    ) : DetailsBottomSheetScreen()

    object TrackingSheet : DetailsBottomSheetScreen()
    object ExternalLinksSheet : DetailsBottomSheetScreen()
    object MergeSheet : DetailsBottomSheetScreen()
    object ArtworkSheet : DetailsBottomSheetScreen()
    object FilterChapterSheet : DetailsBottomSheetScreen()
    class TrackingSearchSheet(val trackingService: TrackService, val alreadySelectedTrack: Track?) : DetailsBottomSheetScreen()
    class TrackingDateSheet(
        val trackAndService: TrackingConstants.TrackAndService,
        val trackingDate: TrackingConstants.TrackingDate,
        val trackSuggestedDates: TrackingConstants.TrackingSuggestedDates?,
    ) : DetailsBottomSheetScreen()
}

@Composable
fun DetailsBottomSheet(
    currentScreen: DetailsBottomSheetScreen,
    themeColorState: ThemeColorState,
    inLibrary: Boolean,
    allCategories: List<Category>,
    mangaCategories: List<Category>,
    addNewCategory: (String) -> Unit,
    loggedInTrackingServices: List<TrackService>,
    tracks: List<Track>,
    dateFormat: DateFormat,
    title: String,
    altTitles: List<String>,
    trackActions: MangaConstants.TrackActions,
    trackSearchResult: TrackingConstants.TrackSearchResult,
    trackSuggestedDates: TrackingConstants.TrackingSuggestedDates?,
    externalLinks: List<ExternalLink>,
    alternativeArtwork: List<Artwork>,
    isMergedManga: MergeConstants.IsMergedManga,
    mergeSearchResult: MergeConstants.MergeSearchResult,
    openInWebView: (String, String) -> Unit,
    coverActions: MangaConstants.CoverActions,
    mergeActions: MangaConstants.MergeActions,
    chapterSortFilter: MangaConstants.SortFilter,
    chapterFilter: MangaConstants.Filter,
    scanlatorFilter: MangaConstants.ScanlatorFilter,
    hideTitlesFilter: Boolean,
    chapterFilterActions: MangaConstants.ChapterFilterActions,
    openSheet: (DetailsBottomSheetScreen) -> Unit,
    closeSheet: () -> Unit,
) {
    val context = LocalContext.current
    when (currentScreen) {
        is DetailsBottomSheetScreen.CategoriesSheet -> EditCategorySheet(
            addingToLibrary = currentScreen.addingToLibrary,
            categories = allCategories,
            mangaCategories = mangaCategories,
            themeColorState = themeColorState,
            cancelClick = closeSheet,
            addNewCategory = addNewCategory,
            confirmClicked = currentScreen.setCategories,
            addToLibraryClick = currentScreen.addToLibraryClick,
        )
        is DetailsBottomSheetScreen.TrackingSheet -> TrackingSheet(
            themeColor = themeColorState,
            services = loggedInTrackingServices,
            tracks = tracks,
            dateFormat = dateFormat,
            onLogoClick = openInWebView,
            onSearchTrackClick = { service, track ->
                closeSheet()
                openSheet(
                    DetailsBottomSheetScreen.TrackingSearchSheet(service, track),
                )
            },
            trackStatusChanged = trackActions.statusChange,
            trackScoreChanged = trackActions.scoreChange,
            trackingRemoved = trackActions.remove,
            trackChapterChanged = trackActions.chapterChange,
            trackingStartDateClick = { trackAndService, trackingDate ->
                closeSheet()
                openSheet(
                    DetailsBottomSheetScreen.TrackingDateSheet(trackAndService, trackingDate, trackSuggestedDates),
                )
            },
            trackingFinishDateClick = { trackAndService, trackingDate ->
                closeSheet()
                openSheet(
                    DetailsBottomSheetScreen.TrackingDateSheet(trackAndService, trackingDate, trackSuggestedDates),
                )
            },
        )
        is DetailsBottomSheetScreen.TrackingSearchSheet -> {
            //do the initial search this way we dont need to "reset" the state after the sheet closes
            LaunchedEffect(key1 = currentScreen.trackingService.id) {
                trackActions.search(title, currentScreen.trackingService)
            }

            TrackingSearchSheet(
                themeColorState = themeColorState,
                title = title,
                trackSearchResult = trackSearchResult,
                alreadySelectedTrack = currentScreen.alreadySelectedTrack,
                service = currentScreen.trackingService,
                cancelClick = {
                    closeSheet()
                    openSheet(DetailsBottomSheetScreen.TrackingSheet)
                },
                searchTracker = { query -> trackActions.search(query, currentScreen.trackingService) },
                openInBrowser = openInWebView,
                trackingRemoved = trackActions.remove,
                trackSearchItemClick = { trackSearch ->
                    closeSheet()
                    trackActions.searchItemClick(TrackingConstants.TrackAndService(trackSearch, currentScreen.trackingService))
                    openSheet(DetailsBottomSheetScreen.TrackingSheet)
                },
            )
        }
        is DetailsBottomSheetScreen.TrackingDateSheet -> {
            TrackingDateSheet(
                themeColorState = themeColorState,
                trackAndService = currentScreen.trackAndService,
                trackingDate = currentScreen.trackingDate,
                trackSuggestedDates = currentScreen.trackSuggestedDates,
                onDismiss = {
                    closeSheet()
                    openSheet(DetailsBottomSheetScreen.TrackingSheet)
                },
                trackDateChanged = { trackDateChanged ->
                    closeSheet()
                    trackActions.dateChange(trackDateChanged)
                    openSheet(DetailsBottomSheetScreen.TrackingSheet)
                },
            )
        }
        is DetailsBottomSheetScreen.ExternalLinksSheet -> {
            ExternalLinksSheet(
                themeColorState = themeColorState, externalLinks = externalLinks,
                onLinkClick = { url, title ->
                    closeSheet()
                    openInWebView(url, title)
                },
            )
        }

        is DetailsBottomSheetScreen.MergeSheet -> {
            if (isMergedManga is MergeConstants.IsMergedManga.No) {
                LaunchedEffect(key1 = 1) {
                    mergeActions.search(title)
                }
            }
            MergeSheet(
                themeColorState = themeColorState,
                isMergedManga = isMergedManga,
                title = title,
                altTitles = altTitles,
                mergeSearchResults = mergeSearchResult,
                openMergeSource = { url, title ->
                    closeSheet()
                    openInWebView(url, title)
                },
                removeMergeSource = {
                    closeSheet()
                    mergeActions.remove()
                },
                cancelClick = {
                    closeSheet()
                },
                search = mergeActions.search,
                mergeMangaClick = { mergeManga ->
                    closeSheet()
                    mergeActions.add(mergeManga)
                },
            )
        }

        is DetailsBottomSheetScreen.ArtworkSheet -> {
            ArtworkSheet(
                themeColorState = themeColorState,
                alternativeArtwork = alternativeArtwork,
                inLibrary = inLibrary,
                saveClick = coverActions.save,
                shareClick = { url -> coverActions.share(context, url) },
                setClick = { url ->
                    closeSheet()
                    coverActions.set(url)
                },
                resetClick = {
                    closeSheet()
                    coverActions.reset()
                },
            )
        }
        is DetailsBottomSheetScreen.FilterChapterSheet -> {
            FilterChapterSheet(
                themeColorState = themeColorState,
                sortFilter = chapterSortFilter,
                changeSort = chapterFilterActions.changeSort,
                changeFilter = chapterFilterActions.changeFilter,
                filter = chapterFilter,
                scanlatorFilter = scanlatorFilter,
                hideTitlesFilter = hideTitlesFilter,
                changeScanlatorFilter = chapterFilterActions.changeScanlator,
                changeHideTitles = chapterFilterActions.hideTitles,
                setAsGlobal = chapterFilterActions.setAsGlobal,
            )
        }
    }
}