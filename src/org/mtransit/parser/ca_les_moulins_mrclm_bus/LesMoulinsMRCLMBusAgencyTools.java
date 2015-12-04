package org.mtransit.parser.ca_les_moulins_mrclm_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
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

// https://www.amt.qc.ca/en/about/open-data
// http://www.amt.qc.ca/xdata/mrclm/google_transit.zip
public class LesMoulinsMRCLMBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-les-moulins-mrclm-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new LesMoulinsMRCLMBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating MRCLM bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating MRCLM bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern SECTEUR = Pattern.compile("(secteur[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String SECTEUR_REPLACEMENT = "";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = SECTEUR.matcher(routeLongName).replaceAll(SECTEUR_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String RSN_24C = "24C";

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (RSN_24C.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RSN_24C;
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		matcher.find();
		return matcher.group();
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		if (RSN_24C.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return 3000l + 24l;
		}
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return Long.parseLong(gRoute.getRouteShortName());
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteId());
		matcher.find();
		return Long.parseLong(matcher.group());
	}

	private static final String AGENCY_COLOR = "99CC00"; // green

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			if (RSN_24C.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "754740";
			}
			Matcher matcher = DIGITS.matcher(gRoute.getRouteId());
			matcher.find();
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
			System.out.printf("\nUnexpected route color %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return super.getRouteColor(gRoute);
	}

	private static final String BOIS_DES_FILION = "Bois-Des-Filion";
	private static final String MASCOUCHE = "Mascouche";
	private static final String MONTREAL = "Montréal";
	private static final String TERREBONNE = "Terrebonne";
	private static final String CÉGEP = "Cégep";
	private static final String TERREBONNE_OUEST = TERREBONNE + " Ouest";
	private static final String TERMINUS_TERREBONNE = "Terminus " + TERREBONNE;
	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1l, new RouteTripSpec(1l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MASCOUCHE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "TER179D", //
								"TER13B", //
								"TER111A", "TER14A", //
								"TER224B", "TER14C", //
								"TER15D", //
								"LCN315A"/* "TER5E" */, "MAS6G" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "MAS6G", "TER20H", //
								"TER14B", //
								"TER110A", "TER224D", //
								"TER111C", //
								"TER13D", //
								"TER179D" })) //
				.compileBothTripSort());
		map2.put(2l, new RouteTripSpec(2l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MASCOUCHE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "TER179D", //
								"MAS132L", //
								"MAS176A", "MAS234D", //
								"MAS231D", "MAS234A", //
								"MAS346B", "MAS58B" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "MAS58B", /* "LCN52C" */"LCN156C", "TER179D" })) //
				.compileBothTripSort());
		map2.put(3l, new RouteTripSpec(3l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MASCOUCHE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "TER179D", "MAS3A", "MAS58D" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "MAS58D", "MAS46A", //
								"MAS234B", //
								"MAS232C", "MAS231B", //
								"MAS176C", "MAS173C", //
								"LCN79C", //
								"TER179D" })) //
				.compileBothTripSort());
		map2.put(5l, new RouteTripSpec(5l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BOIS_DES_FILION) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "BDF12A", "TER129D", "TER179D" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "TER179D", "BDF4C", "BDF12A" })) //
				.compileBothTripSort());
		map2.put(9l, new RouteTripSpec(9l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERMINUS_TERREBONNE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE_OUEST) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "TER260B", /* "TER395C", */"TER281D", /* "TER105D", */"TER179D" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "TER179D", /* "TER37C", "TER104B", */"TER281D", /* "TER105D", */"TER260B" })) //
				.compileBothTripSort());
		map2.put(20l, new RouteTripSpec(20l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MASCOUCHE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "TER179D", "MAS233A", "MAS172A" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "MAS172A", "MAS49C", "TER179D" })) //
				.compileBothTripSort());
		map2.put(23l, new RouteTripSpec(23l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERMINUS_TERREBONNE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CÉGEP) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "STR11C", "TER5C", "TER179D" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "TER179D", "TER133A", "STR11C" })) //
				.compileBothTripSort());
		map2.put(25l, new RouteTripSpec(25l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MONTREAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "MTL6C", //
								"MTN6C", //
								"MTN6A", //
								"MTN19C", //
								"MTN20A", "LVL9A", //
								"TER2A", "TER179D" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "TER179D", //
								"TER2C", "LVL7B", //
								"MTN19C", // old
								"MTN6C", //
								"MTN12C", //
								"MTN12C", //
								"MTL2B", "MTL6C" })) //
				.compileBothTripSort());
		map2.put(41l, new RouteTripSpec(41l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MASCOUCHE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* "TER5E" */"LCN315A", "MAS6G", //
								"TER14B", //
								"TER110A", "TER224D", //
								"TER111C", //
								"TER13D", //
								"TER179D" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(45l, new RouteTripSpec(45l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BOIS_DES_FILION) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "BDF16A", "BDF12A", "TER179D" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (mRoute.getId() == 18l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString("CW", gTrip.getDirectionId());
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString("CCW", gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.getId() == 21l) {
			String gTripHeadsignLC = gTrip.getTripHeadsign().toLowerCase(Locale.ENGLISH);
			if (gTripHeadsignLC.endsWith("mascouche")) {
				mTrip.setHeadsignString(MASCOUCHE, 0);
				return;
			} else if (gTripHeadsignLC.endsWith("terrebonne")) {
				mTrip.setHeadsignString(TERREBONNE, 1);
				return;
			}
			System.out.printf("\nUnexpected trip to split %s!\n", gTrip);
			System.exit(-1);
			return;
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);
	private static final String DIRECTION_REPLACEMENT = "";

	private static final Pattern CHEMIN = Pattern.compile("(chemin )", Pattern.CASE_INSENSITIVE);
	private static final String CHEMIN_REPLACEMENT = "";

	private static final Pattern PARCOURS = Pattern.compile("(parcours )", Pattern.CASE_INSENSITIVE);
	private static final String PARCOURS_REPLACEMENT = "";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = DIRECTION.matcher(tripHeadsign).replaceAll(DIRECTION_REPLACEMENT);
		tripHeadsign = CHEMIN.matcher(tripHeadsign).replaceAll(CHEMIN_REPLACEMENT);
		tripHeadsign = PARCOURS.matcher(tripHeadsign).replaceAll(PARCOURS_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[] { START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE };

	private static final Pattern[] SPACE_FACES = new Pattern[] { SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE };

	private static final Pattern AVENUE = Pattern.compile("( avenue)", Pattern.CASE_INSENSITIVE);
	private static final String AVENUE_REPLACEMENT = " av.";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = AVENUE.matcher(gStopName).replaceAll(AVENUE_REPLACEMENT);
		gStopName = Utils.replaceAll(gStopName, START_WITH_FACES, CleanUtils.SPACE);
		gStopName = Utils.replaceAll(gStopName, SPACE_FACES, CleanUtils.SPACE);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@Override
	public String getStopCode(GStop gStop) {
		if ("0".equals(gStop.getStopCode())) {
			return null;
		}
		return super.getStopCode(gStop);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(GStop gStop) {
		if (gStop.getStopId().equals("LPL105A")) {
			return 84315;
		}
		String stopCode = getStopCode(gStop);
		if (stopCode != null && stopCode.length() > 0) {
			return Integer.valueOf(stopCode); // using stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(gStop.getStopId());
		matcher.find();
		int digits = Integer.parseInt(matcher.group());
		int stopId;
		if (gStop.getStopId().startsWith("MAS")) {
			stopId = 100000;
		} else {
			System.out.println("Stop doesn't have an ID (start with)! " + gStop);
			System.exit(-1);
			stopId = -1;
		}
		if (gStop.getStopId().endsWith("A")) {
			stopId += 1000;
		} else if (gStop.getStopId().endsWith("B")) {
			stopId += 2000;
		} else if (gStop.getStopId().endsWith("C")) {
			stopId += 3000;
		} else if (gStop.getStopId().endsWith("D")) {
			stopId += 4000;
		} else {
			System.out.println("Stop doesn't have an ID (end with)! " + gStop);
			System.exit(-1);
		}
		return stopId + digits;
	}
}
