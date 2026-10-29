// CG-130: the numbers behind the verdict, for the help ("Come decidiamo"). They copy the
// constants of ConditionsScorer and WeatherReducer (crags) and of FerrataScorer and FerrataReducer
// (via ferratas): the backend's HelpRulesTest reads this file and fails when one differs, or when a
// scorer gains a constant this file does not list. Change a threshold there, change it here.
export const CRAG = {
  climbFromHour: 8,
  climbToHour: 18,
  rainDuringDayNoGoMm: 2,
  recentRainSunnyMm: 10,
  windNoGoKmh: 50,
  tooColdC: 5,
  tooHotC: 32,
};

export const FERRATA = {
  fromHour: 7,
  toHour: 19,
  thunderstormHoursNoGo: 1,
  capeNoGoJkg: 1000,
  capeMaybeJkg: 300,
  rainDuringDayNoGoMm: 1,
  freshSnowNoGoCm: 1,
  rainBeforeFrostMm: 2,
  frostC: 0,
  gustNoGoKmh: 60,
  windMaybeKmh: 35,
  recentRainMaybeMm: 5,
  coldMaybeC: 3,
  hotMaybeC: 30,
  shortDayH: 10,
};
