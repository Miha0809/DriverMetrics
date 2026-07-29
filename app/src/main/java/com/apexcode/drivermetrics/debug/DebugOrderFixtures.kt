package com.apexcode.drivermetrics.debug

import com.apexcode.drivermetrics.core.model.TaxiOrder
import com.apexcode.drivermetrics.parser.bolt.BoltParser
import com.apexcode.drivermetrics.parser.uber.UberParser

/**
 * TEMPORARY, debug-only: builds TaxiOrder objects by running the real parsers against the exact
 * text captured from the sample screenshots in ui/examples_screen, so the debug screen can
 * trigger the full geocode -> route -> metrics -> overlay pipeline without needing to catch a
 * live order in Bolt/Uber. Delete this whole file (and its use in MainActivity) once it's no
 * longer needed.
 */
object DebugOrderFixtures {

    data class Fixture(val label: String, val order: TaxiOrder)

    val all: List<Fixture> by lazy {
        listOfNotNull(
            BoltParser().parseFromTexts(BOLT_SCREEN_1)?.let { Fixture("Bolt — скрін 1", it) },
            BoltParser().parseFromTexts(BOLT_SCREEN_2)?.let { Fixture("Bolt — скрін 2", it) },
            UberParser().parseFromTexts(UBER_SCREEN_1)?.let { Fixture("Uber — скрін 1 (RU)", it) },
            UberParser().parseFromTexts(UBER_SCREEN_2)?.let { Fixture("Uber — скрін 2 (UK)", it) },
        )
    }

    private val BOLT_SCREEN_1 = listOf(
        "Bolt",
        "zł 39.28 (NET, tax included)",
        "Magdalena • 4.9 ★",
        "5 min • 2 km",
        "Herbu Janina 3, Warszawa 02-972",
        "30 min • 13.3 km",
        "Podwale 25 Kompania Piwna, Podwale 25, Warszawa 00-261",
        "Decline",
        "Confirm",
    )

    private val BOLT_SCREEN_2 = listOf(
        "Bolt",
        "1.2x wysoki popyt",
        "Poza zasięgiem",
        "14,91 zł (netto z podatkiem)",
        "Odrzucenie nie wpłynie na Wskaźnik akceptacji",
        "Yevgenii • 5.0 ★",
        "6 min • 3.8 km",
        "Jagiellońska 1, Częstochowa 42-216",
        "8 min • 4.5 km",
        "Mikołaja Kopernika 33, Częstochowa",
        "Odrzuć",
        "Akceptuję",
    )

    private val UBER_SCREEN_1 = listOf(
        "UberX",
        "49,30 PLN",
        "4,83 ★",
        "С вычетом сервисного сбора, в т.ч. НДС",
        "В 8 мин. (3.1 км) от вас",
        "aleja Władysława S. Reymonta, Варшава",
        "Поездка: 49 мин. (31.2 км)",
        "ulica gen. Wiktora Thommee 1A, Nowy Dwór Mazowiecki",
        "Долгая поездка (более 35 мин.)",
        "Принять",
    )

    private val UBER_SCREEN_2 = listOf(
        "UberX",
        "Ексклюзивний",
        "50,67 PLN",
        "4,83 ★",
        "Сервісний збір без податків (зокрема без ПДВ на н",
        "За 10 хв (3.7 км)",
        "aleja Władysława S. Reymonta, Варшава",
        "Поїздка: 49 хв (31.2 км)",
        "ulica gen. Wiktora Thommee 1A, Nowy Dwór Mazowiecki",
        "Тривала поїздка (понад 35 хв)",
        "Прийняти",
    )
}
