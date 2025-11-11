/**
 * Department Names Utility
 *
 * Provides helper functions to get French department names from department codes.
 * This utility is used across the application for consistent department name display.
 *
 * Usage:
 * ```typescript
 * import { getDepartmentName, DEPARTMENT_NAMES } from '@/shared/utils/department-names.util';
 *
 * const deptName = getDepartmentName('75'); // Returns 'Paris'
 * const unknownDept = getDepartmentName('99'); // Returns 'Département 99'
 * ```
 */

/**
 * Comprehensive mapping of French department codes to their full names.
 * Includes all 101 departments (96 metropolitan + 5 overseas).
 */
export const DEPARTMENT_NAMES: { [key: string]: string } = {
  // Île-de-France
  "75": "Paris",
  "77": "Seine-et-Marne",
  "78": "Yvelines",
  "91": "Essonne",
  "92": "Hauts-de-Seine",
  "93": "Seine-Saint-Denis",
  "94": "Val-de-Marne",
  "95": "Val-d'Oise",

  // Auvergne-Rhône-Alpes
  "01": "Ain",
  "03": "Allier",
  "07": "Ardèche",
  "15": "Cantal",
  "26": "Drôme",
  "38": "Isère",
  "42": "Loire",
  "43": "Haute-Loire",
  "63": "Puy-de-Dôme",
  "69": "Rhône",
  "73": "Savoie",
  "74": "Haute-Savoie",

  // Bourgogne-Franche-Comté
  "21": "Côte-d'Or",
  "25": "Doubs",
  "39": "Jura",
  "58": "Nièvre",
  "70": "Haute-Saône",
  "71": "Saône-et-Loire",
  "89": "Yonne",
  "90": "Territoire de Belfort",

  // Bretagne
  "22": "Côtes-d'Armor",
  "29": "Finistère",
  "35": "Ille-et-Vilaine",
  "56": "Morbihan",

  // Centre-Val de Loire
  "18": "Cher",
  "28": "Eure-et-Loir",
  "36": "Indre",
  "37": "Indre-et-Loire",
  "41": "Loir-et-Cher",
  "45": "Loiret",

  // Corse
  "2A": "Corse-du-Sud",
  "2B": "Haute-Corse",

  // Grand Est
  "08": "Ardennes",
  "10": "Aube",
  "51": "Marne",
  "52": "Haute-Marne",
  "54": "Meurthe-et-Moselle",
  "55": "Meuse",
  "57": "Moselle",
  "67": "Bas-Rhin",
  "68": "Haut-Rhin",
  "88": "Vosges",

  // Hauts-de-France
  "02": "Aisne",
  "59": "Nord",
  "60": "Oise",
  "62": "Pas-de-Calais",
  "80": "Somme",

  // Normandie
  "14": "Calvados",
  "27": "Eure",
  "50": "Manche",
  "61": "Orne",
  "76": "Seine-Maritime",

  // Nouvelle-Aquitaine
  "16": "Charente",
  "17": "Charente-Maritime",
  "19": "Corrèze",
  "23": "Creuse",
  "24": "Dordogne",
  "33": "Gironde",
  "40": "Landes",
  "47": "Lot-et-Garonne",
  "64": "Pyrénées-Atlantiques",
  "79": "Deux-Sèvres",
  "86": "Vienne",
  "87": "Haute-Vienne",

  // Occitanie
  "09": "Ariège",
  "11": "Aude",
  "12": "Aveyron",
  "30": "Gard",
  "31": "Haute-Garonne",
  "32": "Gers",
  "34": "Hérault",
  "46": "Lot",
  "48": "Lozère",
  "65": "Hautes-Pyrénées",
  "66": "Pyrénées-Orientales",
  "81": "Tarn",
  "82": "Tarn-et-Garonne",

  // Pays de la Loire
  "44": "Loire-Atlantique",
  "49": "Maine-et-Loire",
  "53": "Mayenne",
  "72": "Sarthe",
  "85": "Vendée",

  // Provence-Alpes-Côte d'Azur
  "04": "Alpes-de-Haute-Provence",
  "05": "Hautes-Alpes",
  "06": "Alpes-Maritimes",
  "13": "Bouches-du-Rhône",
  "83": "Var",
  "84": "Vaucluse",

  // Départements et régions d'outre-mer (DROM)
  "971": "Guadeloupe",
  "972": "Martinique",
  "973": "Guyane",
  "974": "La Réunion",
  "976": "Mayotte",
};

/**
 * Gets the full department name from a department code.
 *
 * @param code Department code (e.g., '75' for Paris, '13' for Bouches-du-Rhône)
 * @returns Full department name or fallback "Département {code}" if code not found
 *
 * @example
 * getDepartmentName('75'); // Returns 'Paris'
 * getDepartmentName('13'); // Returns 'Bouches-du-Rhône'
 * getDepartmentName('2A'); // Returns 'Corse-du-Sud'
 * getDepartmentName('99'); // Returns 'Département 99' (unknown code)
 * getDepartmentName(''); // Returns 'Département inconnu' (empty code)
 */
export function getDepartmentName(code: string): string {
  if (!code || code.trim() === "") {
    return "Département inconnu";
  }

  const trimmedCode = code.trim();
  return DEPARTMENT_NAMES[trimmedCode] || `Département ${trimmedCode}`;
}

/**
 * Gets a short version of the department name (first 20 characters).
 * Useful for display in limited space (e.g., mobile views, tooltips).
 *
 * @param code Department code
 * @param maxLength Maximum length of returned string (default: 20)
 * @returns Shortened department name with ellipsis if truncated
 *
 * @example
 * getDepartmentShortName('13'); // Returns 'Bouches-du-Rhône'
 * getDepartmentShortName('04'); // Returns 'Alpes-de-Haute-Pro...'
 */
export function getDepartmentShortName(code: string, maxLength: number = 20): string {
  const fullName = getDepartmentName(code);

  if (fullName.length <= maxLength) {
    return fullName;
  }

  return fullName.substring(0, maxLength - 3) + "...";
}

/**
 * Checks if a department code exists in the mapping.
 *
 * @param code Department code to check
 * @returns true if code exists, false otherwise
 *
 * @example
 * isValidDepartmentCode('75'); // Returns true
 * isValidDepartmentCode('99'); // Returns false
 */
export function isValidDepartmentCode(code: string): boolean {
  return code !== null && code !== undefined && DEPARTMENT_NAMES[code.trim()] !== undefined;
}

/**
 * Gets all department codes.
 * Useful for dropdowns, autocomplete, or validation.
 *
 * @returns Array of all valid department codes
 *
 * @example
 * const codes = getAllDepartmentCodes();
 * // Returns ['01', '02', '03', ..., '2A', '2B', ..., '976']
 */
export function getAllDepartmentCodes(): string[] {
  return Object.keys(DEPARTMENT_NAMES);
}

/**
 * Gets all departments as an array of objects with code and name.
 * Useful for dropdowns or selects.
 *
 * @returns Array of { code, name } objects sorted by code
 *
 * @example
 * const departments = getAllDepartments();
 * // Returns [{ code: '01', name: 'Ain' }, { code: '02', name: 'Aisne' }, ...]
 */
export function getAllDepartments(): Array<{ code: string; name: string }> {
  return Object.entries(DEPARTMENT_NAMES)
    .map(([code, name]) => ({ code, name }))
    .sort((a, b) => a.code.localeCompare(b.code));
}
