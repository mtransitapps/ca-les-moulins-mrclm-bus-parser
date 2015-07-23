package org.mtransit.parser.ca_les_moulins_mrclm_bus;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

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

	@Override
	public String getRouteShortName(GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		matcher.find();
		return matcher.group();
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.getRouteId());
		matcher.find();
		return Integer.parseInt(matcher.group());
	}

	private static final String AGENCY_COLOR = "99CC00"; // green

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String HENRI_BOURASSA = "Henri-Bourassa";
	private static final String TERMINUS_TERREBONNE_CÉGEP = "Terminus Terrebonne (Cégep)";
	private static final String CÉGEP = "Cégep";
	private static final String TERMINUS_TERREBONNE = "Terminus Terrebonne";
	private static final String MASCOUCHE_TERREBONNE = "Mascouche <-> Terrebonne";
	private static final String BOIS_DES_FILION_TERREBONNE = "Bois-Des-Filion <-> Terrebonne";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		String stationName = cleanTripHeadsign(gTrip.getTripHeadsign());
		if (mRoute.id == 5l) {
			stationName = BOIS_DES_FILION_TERREBONNE;
		} else if (mRoute.id == 20l) {
			stationName = MASCOUCHE_TERREBONNE;
		} else if (mRoute.id == 23l) {
			if (gTrip.getDirectionId() == 0) {
				stationName = TERMINUS_TERREBONNE;
			} else {
				stationName = CÉGEP;
			}
		} else if (mRoute.id == 24l) {
			if (gTrip.getDirectionId() == 0) {
				stationName = TERMINUS_TERREBONNE_CÉGEP;
			}
		} else if (mRoute.id == 25l) {
			if (gTrip.getDirectionId() == 1) {
				stationName = HENRI_BOURASSA;
			}
		}
		mTrip.setHeadsignString(stationName, gTrip.getDirectionId());
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
		return super.cleanStopNameFR(gStopName);
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
		// generating integer stop ID
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
