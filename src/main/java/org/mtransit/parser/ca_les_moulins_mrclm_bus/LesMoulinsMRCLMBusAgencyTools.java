package org.mtransit.parser.ca_les_moulins_mrclm_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://exo.quebec/en/about/open-data
// https://exo.quebec/xdata/mrclm/google_transit.zip
public class LesMoulinsMRCLMBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-les-moulins-mrclm-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new LesMoulinsMRCLMBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating MRCLM bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating MRCLM bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern SECTEUR = Pattern.compile("(secteur[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String SECTEUR_REPLACEMENT = "";

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongNameOrDefault();
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = SECTEUR.matcher(routeLongName).replaceAll(SECTEUR_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String B = "B";
	private static final String C = "C";
	private static final String G = "G";
	private static final String T = "T";

	private static final String MT = "MT";

	private static final long RID_ENDS_WITH_B = 2_000L;
	private static final long RID_ENDS_WITH_C = 3_000L;
	private static final long RID_ENDS_WITH_G = 7_000L;

	private static final long RID_STARTS_WITH_T = 20_000L;

	private static final long RID_STARTS_WITH_MT = 1_320_000L;

	private static final String RSN_EXPH = "EXPH";
	private static final String RSN_EXPM = "EXPM";
	private static final String RSN_EXPR = "EXPR";

	private static final long RID_EXPH = 99_001L;
	private static final long RID_EXPM = 99_002L;
	private static final long RID_EXPR = 99_003L;

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		//noinspection deprecation
		if (!Utils.isDigitsOnly(gRoute.getRouteId())) {
			if (RSN_EXPH.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return RID_EXPH;
			} else if (RSN_EXPM.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return RID_EXPM;
			} else if (RSN_EXPR.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return RID_EXPR;
			}
			Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
			if (matcher.find()) {
				int digits = Integer.parseInt(matcher.group());
				if (gRoute.getRouteShortName().startsWith(MT)) {
					return RID_STARTS_WITH_MT + digits;
				}
				if (gRoute.getRouteShortName().startsWith(T)) {
					return RID_STARTS_WITH_T + digits;
				}
				if (gRoute.getRouteShortName().endsWith(B)) {
					return RID_ENDS_WITH_B + digits;
				} else if (gRoute.getRouteShortName().endsWith(C)) {
					return RID_ENDS_WITH_C + digits;
				} else if (gRoute.getRouteShortName().endsWith(G)) {
					return RID_ENDS_WITH_G + digits;
				}
			}
			throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
		}
		return super.getRouteId(gRoute);
	}

	private static final String AGENCY_COLOR = "1F1F1F"; // DARK GRAY (from GTFS)

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			if ("24C".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "754740";
			}
			//noinspection deprecation
			Matcher matcher = DIGITS.matcher(gRoute.getRouteId());
			if (matcher.find()) {
				int rid = Integer.parseInt(matcher.group());
				switch (rid) {
				// @formatter:off
				case 1: return "1A5846";
				case 2: return "EC2F48";
				case 3: return "EC2F48";
				case 4: return "EC008C";
				case 5: return "E46D1E";
				case 6: return "000000";
				case 8: return "231F20";
				case 9: return "B3CB2D";
				case 11: return "26ABDF";
				case 14: return "9FA1A4";
				case 15: return "F6ABC9";
				case 16: return "9C3F97";
				case 17: return "F6ABC9";
				case 18: return "F8B43A";
				case 19: return "8AB5E1";
				case 20: return "028C5B";
				case 21: return "1D407D";
				case 22: return "92B02C";
				case 23: return "A0092D";
				case 24: return "2A465A";
				case 25: return "26225C";
				case 26: return "000000";
				case 27: return "008EBE";
				case 30: return "6F6D70";
				case 35: return "26225C";
				case 40: return "811B55";
				case 41: return "C19708";
				case 42: return "08B6AD";
				case 43: return "08B6AD";
				case 45: return "684B1F";
				case 48: return "C79EC9";
				case 55: return "000000";
				case 57: return "000000";
				case 124: return "000000";
				case 125: return "000000";
				case 140: return "A85A29";
				case 403: return "A71F67";
				case 411: return "38BEAC";
				case 417: return "0096A9";
				case 418: return "5D6335";
				case 427: return "778937";
				case 440: return "5F7975";
				// @formatter:on
				}
			}
			throw new MTLog.Fatal("Unexpected route color %s!", gRoute);
		}
		return super.getRouteColor(gRoute);
	}

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		//noinspection deprecation
		map2.put(18L, new RouteTripSpec(18L, // BECAUSE SAME HEAD-SIGNs
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Term Terrebonne", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Cité du Sport") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"85782", // Cité du Sport
								"84106", // rue des Bâtisseurs / face au 3100
								"84728", // ==
								"85117", // !=== !=
								"85482", // != <>
								"85144", // != !=
								"85030", // !===
								"84889", // !===
								"84837", // !===
								"84875" // == Terminus Terrebonne
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"84875", // == Terminus Terrebonne
								"84943", // !=== boul. des Seigneurs / rue Vaillant
								"85117", // != !=
								"85482", // != <>
								"84726", // !=== boul. Claude Léveillée / face au McDonald
								"84845", // !=== boul. Moody / face aux Galeries Terrebonne
								"84998", // != rue Angora / ch. Gasco
								"85150", // !=== boul. des Entreprises / boul. Claude Léveillée
								"85782" // == Cité du Sport
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@NotNull
	@Override
	public String cleanStopOriginalId(@NotNull String gStopId) {
		gStopId = CleanUtils.cleanMergedID(gStopId);
		return gStopId;
	}

	@Override
	public int compareEarly(long routeId, @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2, @NotNull MTripStop ts1, @NotNull MTripStop ts2, @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull ArrayList<MTrip> splitTrips, @NotNull GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	@Override
	public boolean directionFinderEnabled(long routeId, @NotNull GRoute gRoute) {
		if (routeId == 18L) {
			return false; // DISABLED because 2 directions with same head-sign, same last stop, different stops order
		}
		return super.directionFinderEnabled(routeId, gRoute);
	}

	private static final Pattern STARTS_WITH_CITY_SLASH_ = Pattern.compile("(^(.*) / (.*)$)", Pattern.CASE_INSENSITIVE);
	private static final String STARTS_WITH_CITY_SLASH_REPLACEMENT = "$3 / $2";

	@NotNull
	@Override
	public String cleanDirectionHeadsign(boolean fromStopName, @NotNull String directionHeadSign) {
		directionHeadSign = super.cleanDirectionHeadsign(fromStopName, directionHeadSign);
		directionHeadSign = STARTS_WITH_CITY_SLASH_.matcher(directionHeadSign).replaceAll(STARTS_WITH_CITY_SLASH_REPLACEMENT);
		return directionHeadSign;
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 18L) {
			if (Arrays.asList( //
					"Angora / Gascon", //
					"Terrebonne / Cegep De Terrebonne" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Terrebonne / Cegep De Terrebonne", mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);
	private static final String DIRECTION_REPLACEMENT = "";

	private static final Pattern CHEMIN = Pattern.compile("(chemin )", Pattern.CASE_INSENSITIVE);
	private static final String CHEMIN_REPLACEMENT = "";

	private static final Pattern EXPRESS_ = Pattern.compile("(expresse|express )", Pattern.CASE_INSENSITIVE);
	private static final String EXPRESS_REPLACEMENT = "";

	private static final Pattern VERS_ = Pattern.compile("(vers )", Pattern.CASE_INSENSITIVE);
	private static final String VERS_REPLACEMENT = "";

	private static final Pattern PARCOURS = Pattern.compile("(parcours )", Pattern.CASE_INSENSITIVE);
	private static final String PARCOURS_REPLACEMENT = "";

	private static final Pattern ENDS_WITH_AM_PM = Pattern.compile("( (am|pm)$)", Pattern.CASE_INSENSITIVE);
	private static final String ENDS_WITH_AM_PM_REPLACEMENT = StringUtils.EMPTY;

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = ENDS_WITH_AM_PM.matcher(tripHeadsign).replaceAll(ENDS_WITH_AM_PM_REPLACEMENT);
		tripHeadsign = DIRECTION.matcher(tripHeadsign).replaceAll(DIRECTION_REPLACEMENT);
		tripHeadsign = EXPRESS_.matcher(tripHeadsign).replaceAll(EXPRESS_REPLACEMENT);
		tripHeadsign = VERS_.matcher(tripHeadsign).replaceAll(VERS_REPLACEMENT);
		tripHeadsign = CHEMIN.matcher(tripHeadsign).replaceAll(CHEMIN_REPLACEMENT);
		tripHeadsign = PARCOURS.matcher(tripHeadsign).replaceAll(PARCOURS_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_ET.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_ET_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanBounds(Locale.FRENCH, tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[]{START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE};

	private static final Pattern[] SPACE_FACES = new Pattern[]{SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE};

	private static final Pattern AVENUE = Pattern.compile("( avenue)", Pattern.CASE_INSENSITIVE);
	private static final String AVENUE_REPLACEMENT = " av.";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = AVENUE.matcher(gStopName).replaceAll(AVENUE_REPLACEMENT);
		gStopName = Utils.replaceAll(gStopName, START_WITH_FACES, CleanUtils.SPACE);
		gStopName = Utils.replaceAll(gStopName, SPACE_FACES, CleanUtils.SPACE);
		gStopName = CleanUtils.cleanBounds(Locale.FRENCH, gStopName);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		if ("0".equals(gStop.getStopCode())) {
			return StringUtils.EMPTY;
		}
		return super.getStopCode(gStop);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(@NotNull GStop gStop) {
		//noinspection deprecation
		final String stopId1 = gStop.getStopId();
		if (stopId1.equals("LPL105A")) {
			return 84315;
		}
		String stopCode = getStopCode(gStop);
		if (stopCode.length() > 0) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(stopId1);
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId;
			if (stopId1.startsWith("MAS")) {
				stopId = 100_000;
			} else if (stopId1.startsWith("TER")) {
				stopId = 200_000;
			} else {
				throw new MTLog.Fatal("Stop doesn't have an ID (start with) %s!", gStop);
			}
			if (stopId1.endsWith("A")) {
				stopId += 1_000;
			} else if (stopId1.endsWith("B")) {
				stopId += 2_000;
			} else if (stopId1.endsWith("C")) {
				stopId += 3_000;
			} else if (stopId1.endsWith("D")) {
				stopId += 4_000;
			} else if (stopId1.endsWith("G")) {
				stopId += 7_000;
			} else {
				throw new MTLog.Fatal("Stop doesn't have an ID (end with) %s!", gStop);
			}
			return stopId + digits;
		}
		throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
	}
}
