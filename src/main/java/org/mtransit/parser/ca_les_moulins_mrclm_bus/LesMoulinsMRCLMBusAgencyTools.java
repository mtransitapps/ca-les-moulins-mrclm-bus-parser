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

// https://rtm.quebec/en/about/open-data
// https://rtm.quebec/xdata/mrclm/google_transit.zip
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
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		System.out.printf("\nGenerating MRCLM bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
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
	public long getRouteId(GRoute gRoute) {
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
			System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
			System.exit(-1);
			return -1L;
		}
		return super.getRouteId(gRoute);
	}

	private static final String AGENCY_COLOR = "1F1F1F"; // DARK GRAY (from GTFS)

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			if ("24C".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "754740";
			}
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
			System.out.printf("\nUnexpected route color %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return super.getRouteColor(gRoute);
	}

	private static final String TERMINUS_SHORT = "Term";
	private static final String BOIS_DES_FILION = "Bois-Des-Filion";
	private static final String MASCOUCHE = "Mascouche";
	private static final String TERREBONNE = "Terrebonne";
	private static final String HENRI_BOURASSA = "Henri-Bourassa";
	private static final String CÉGEP = "Cégep";
	private static final String TERMINUS_HENRI_BOURASSA = TERMINUS_SHORT + " " + HENRI_BOURASSA;
	private static final String TERMINUS_TERREBONNE = TERMINUS_SHORT + " " + TERREBONNE;
	private static final String CITE_DU_SPORT = "Cite Du Sport";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1L, new RouteTripSpec(1L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MASCOUCHE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"84875", // Terminus Terrebonne
								"84800", // == boul. des Seigneurs / rue Plaisance
								"84744", // !==
								"84819", // !==
								"85147", // !==
								"84737", // != rue Cologne / rue de la Pinière
								"85527", // !==
								"84844", // == rue de Verviers / rue Aragon
								"84994", // ch. des Anglais / ch. Gascon
								"85073", // rue des Bois-Francs / ch. Pincourt #MASCOUCHE
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"85073", // rue des Bois-Francs / ch. Pincourt #MASCOUCHE
								"84924", // ch. Gascon / rue de la Pinière
								"84820", // == rue de Verviers / rue Plaisance
								"84742", // !=
								"85148", // !=
								"84745", // !=
								"84801", // == boul. des Seigneurs / rue Plaisance
								"84875", // Terminus Terrebonne
						})) //
				.compileBothTripSort());
		map2.put(2L, new RouteTripSpec(2L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MASCOUCHE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"84875", // Terminus Terrebonne
								"85353", // avenue de l'Esplanade / avenue de la Gare
								"84140", // ch. des Anglais / boul. Ste-Marie
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"84140", // ch. des Anglais / boul. Ste-Marie
								"84142", // ch. des Anglais / rue O'Diana
								"84875", // Terminus Terrebonne
						})) //
				.compileBothTripSort());
		map2.put(3L, new RouteTripSpec(3L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MASCOUCHE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"84875", // Terminus Terrebonne
								"84536", // ch. des Anglais / rue Rawlinson
								"84570", // ch. des Anglais / boul. Ste-Marie
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"84570", // ch. des Anglais / boul. Ste-Marie
								"84362", // == avenue de l'Esplanade / rue Bohémier
								"85773", // !=
								"85183", // !=
								"84251", // !=
								"84144", // !=
								"85054", // == boul. des Seigneurs / rue J.-F.-Kennedy
								"84875", // Terminus Terrebonne
						})) //
				.compileBothTripSort());
		map2.put(5L, new RouteTripSpec(5L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BOIS_DES_FILION) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"84006", // montée Gagnon / ch. du Souvenir
								"84855", // ++
								"84875", // Terminus Terrebonne
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"84875", // Terminus Terrebonne
								"84013", // == ch. Adolphe-Chapleau / 38e avenue sud
								"85521", // !=
								"85520", // != boul. Industriel / rue Jacques Paschini
								"84006", // montée Gagnon / ch. du Souvenir
						})) //
				.compileBothTripSort());
		map2.put(8L, new RouteTripSpec(8L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Angora / Hansen", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERMINUS_TERREBONNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"84875", // Terminus Terrebonne
								"84988", // rue de Grandchamps / boul. de Hauteville
								"84833", // rue Angora / rue Hansen
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"84833", // rue Angora / rue Hansen
								"85030", // ++
								"84875", // Terminus Terrebonne
						})) //
				.compileBothTripSort());
		map2.put(18L, new RouteTripSpec(18L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERMINUS_TERREBONNE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Cité du Sport") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"85782", // Cité du Sport
								"84106", // rue des Bâtisseurs / face au 3100
								// "84872", // ++
								"84728", // ==
								"85117", // !=== !=
								"85482", // != <>
								"85144", // != !=
								"85030", // !===
								"84889", // !===
								"84837", // !===
								"84875", // == Terminus Terrebonne
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"84875", // == Terminus Terrebonne
								"84943", // !=== boul. des Seigneurs / rue Vaillant
								"85117", // != !=
								"85482", // != <>
								"84726", // !=== boul. Claude Léveillée / face au McDonald
								"84845", // !=== boul. Moody / face aux Galeries Terrebonne
								"84998", // != rue Angora / ch. Gasco
								"85150", // !=== boul. des Entreprises / boul. Claude Léveillée
								"85782", // == Cité du Sport
						})) //
				.compileBothTripSort());
		map2.put(20L, new RouteTripSpec(20L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MASCOUCHE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"84875", // Terminus Terrebonne
								"85179", // ++
								"84467", // boul. St-Henri / face au 1533
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"84467", // boul. St-Henri / face au 1533
								"84553", // ++
								"84875", // Terminus Terrebonne
						})) //
				.compileBothTripSort());
		map2.put(23L, new RouteTripSpec(23L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERMINUS_TERREBONNE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CÉGEP) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"84697", // == Cégep Lionel-Groulx
								"84718", // !=
								"85424", // ==
								"84875", // Terminus Terrebonne
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"84875", // Terminus Terrebonne
								"85423", // ==
								"84717", // !=
								"84697", // == Cégep Lionel-Groulx
						})) //
				.compileBothTripSort());
		map2.put(24L + RID_STARTS_WITH_T, new RouteTripSpec(24L + RID_STARTS_WITH_T, // T24
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Gascon", // Terrebonne
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Place Longchamps") // Terrebonne
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"85125", // ch. Martin / montée Valiquette
								"85135", // ++
								"84870", // ch. Gascon / face au 3620
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"85131", // Comptois / Gascon (Restaurant au nid garni )
								"85134", // ++
								"85126", // montée Valiquette / ch. Martin
						})) //
				.compileBothTripSort());
		map2.put(26L + RID_STARTS_WITH_T, new RouteTripSpec(26L + RID_STARTS_WITH_T, // T26
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "St-Philippe & Tedford", // Mascouche
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "St-Henri") // Mascouche
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"84467", // boul. St-Henri / face au 1533
								"84902", // ++
								"84909", // Saint-Philippe Ouest / Tedford
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"84910", // Saint-Philippe Ouest / Tedford
								"84913", // ++
								"84467", // boul. St-Henri / face au 1533
						})) //
				.compileBothTripSort());
		map2.put(41L, new RouteTripSpec(41L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MASCOUCHE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"84994", // ch. des Anglais / ch. Gascon
								"84820", // ==
								"84742", // !=
								"84801", // !=
								"84745", // !=
								"84727", // !=
								"85043", // ==
								"84875", // Terminus Terrebonne
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops */
						})) //
				.compileBothTripSort());
		map2.put(45L, new RouteTripSpec(45L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERREBONNE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BOIS_DES_FILION) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"85491", // rue Henry-Bessemer / rue Italia
								"84006", // montée Gagnon / ch. du Souvenir
								"84875", // Terminus Terrebonne
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops */
						})) //
				.compileBothTripSort());
		map2.put(403L, new RouteTripSpec(403L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Gare Mascouche", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Anglais / Gascon") //
				.addTripSort(0, // MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"84994", // ch. des Anglais / ch. Gascon
								"84570", // ++
								"84700", // Gare Mascouche
						})) //
				.addTripSort(1, // MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"84700", // Gare Mascouche
								"84584", // ++
								"84019", // ch des Anglais/ch. Gascon
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public String cleanStopOriginalId(String gStopId) {
		gStopId = CleanUtils.cleanMergedID(gStopId);
		return gStopId;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
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
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (mRoute.getId() == 14L) {
			String gTripHeadsignLC = gTrip.getTripHeadsign().toLowerCase(Locale.ENGLISH);
			if (gTripHeadsignLC.endsWith("forum de la plaine")) {
				mTrip.setHeadsignString("Forum De La Plaine", gTrip.getDirectionId());
				return;
			} else if (gTripHeadsignLC.endsWith("terrebonne")) {
				mTrip.setHeadsignString(TERREBONNE, gTrip.getDirectionId());
				return;
			}
			System.out.printf("\nUnexpected trip to split %s!\n", gTrip);
			System.exit(-1);
			return;
		} else if (mRoute.getId() == 21L) {
			String gTripHeadsignLC = gTrip.getTripHeadsign().toLowerCase(Locale.ENGLISH);
			if (gTripHeadsignLC.endsWith("mascouche")) {
				mTrip.setHeadsignString(MASCOUCHE, gTrip.getDirectionId());
				return;
			} else if (gTripHeadsignLC.endsWith("terrebonne") //
					|| gTripHeadsignLC.endsWith("terrebonne am") //
					|| gTripHeadsignLC.endsWith("terrebonne pm")) {
				mTrip.setHeadsignString(TERREBONNE, gTrip.getDirectionId());
				return;
			}
			System.out.printf("\nUnexpected trip to split %s!\n", gTrip);
			System.exit(-1);
			return;
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 9L) {
			if (Arrays.asList( //
					"Souvenir / Gagnon", //
					"St-Roch / Lamothe" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("St-Roch / Lamothe", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 11L) {
			if (Arrays.asList( //
					"Lachenaie / Cegep Terrebonne", //
					"Cité Du Sport", //
					TERREBONNE + " " + CITE_DU_SPORT, //
					TERMINUS_TERREBONNE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(TERMINUS_TERREBONNE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 25L) {
			if (Arrays.asList( //
					"St-Julien / Amos", //
					TERMINUS_HENRI_BOURASSA //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(TERMINUS_HENRI_BOURASSA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_STARTS_WITH_T + 55L) { // T55
			if (Arrays.asList( //
					"Terrebonne / Pinière & Sobeys", //
					TERMINUS_TERREBONNE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(TERMINUS_TERREBONNE, mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);
	private static final String DIRECTION_REPLACEMENT = "";

	private static final Pattern CHEMIN = Pattern.compile("(chemin )", Pattern.CASE_INSENSITIVE);
	private static final String CHEMIN_REPLACEMENT = "";

	private static final Pattern EXPRESS_ = Pattern.compile("(expresse|express )", Pattern.CASE_INSENSITIVE);
	private static final String EXPRESS_REPLACEMENT = "";

	private static final Pattern VERS_ = Pattern.compile("(vers )", Pattern.CASE_INSENSITIVE);
	private static final String VERS_REPLACEMENT = "";

	private static final Pattern TERMINUS_ = Pattern.compile("(terminus )", Pattern.CASE_INSENSITIVE);
	private static final String TERMINUS_REPLACEMENT = TERMINUS_SHORT + " ";

	private static final Pattern PARCOURS = Pattern.compile("(parcours )", Pattern.CASE_INSENSITIVE);
	private static final String PARCOURS_REPLACEMENT = "";

	private static final Pattern ENDS_WITH_AM_PM = Pattern.compile("( (am|pm)$)", Pattern.CASE_INSENSITIVE);
	private static final String ENDS_WITH_AM_PM_REPLACEMENT = StringUtils.EMPTY;

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.FRENCH);
		}
		tripHeadsign = ENDS_WITH_AM_PM.matcher(tripHeadsign).replaceAll(ENDS_WITH_AM_PM_REPLACEMENT);
		tripHeadsign = DIRECTION.matcher(tripHeadsign).replaceAll(DIRECTION_REPLACEMENT);
		tripHeadsign = EXPRESS_.matcher(tripHeadsign).replaceAll(EXPRESS_REPLACEMENT);
		tripHeadsign = VERS_.matcher(tripHeadsign).replaceAll(VERS_REPLACEMENT);
		tripHeadsign = TERMINUS_.matcher(tripHeadsign).replaceAll(TERMINUS_REPLACEMENT);
		tripHeadsign = CHEMIN.matcher(tripHeadsign).replaceAll(CHEMIN_REPLACEMENT);
		tripHeadsign = PARCOURS.matcher(tripHeadsign).replaceAll(PARCOURS_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_ET.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_ET_REPLACEMENT);
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
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId;
			if (gStop.getStopId().startsWith("MAS")) {
				stopId = 100000;
			} else if (gStop.getStopId().startsWith("TER")) {
				stopId = 200000;
			} else {
				System.out.printf("\nStop doesn't have an ID (start with) %s!\n", gStop);
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
			} else if (gStop.getStopId().endsWith("G")) {
				stopId += 7000;
			} else {
				System.out.printf("\nStop doesn't have an ID (end with) %s!\n", gStop);
				System.exit(-1);
			}
			return stopId + digits;
		}
		System.out.printf("\nUnexpected stop ID for %s!\n", gStop);
		System.exit(-1);
		return -1;
	}
}
