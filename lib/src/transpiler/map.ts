function decodeBase64(val: number) {
	if (val >= 65 && val <= 90) return val - 65;
	else if (val >= 97 && val <= 122) return val - 97 + 26;
	else if (val >= 48 && val <= 57) return val - 48 + 52;
	else if (val == 43) return 62;
	else if (val == 47) return 63;
	else throw "Invalid Base64 char";
}

export function decodeVLQ(val: string): number[][][] {
	const lines: number[][][] = [];

	const fileParts = val.split(";", -1);

	for (let i = 0; i < fileParts.length; i++) {
		const line = fileParts[i];
		if (line.length === 0) {
			lines.push([]);
			continue;
		}

		const elements: number[][] = [];
		const lineParts = line.split(",", -1);

		for (let i = 0; i < lineParts.length; i++) {
			const el = lineParts[i];

			if (el.length === 0) elements.push([]);
			else {
				const list: number[] = [];

				for (let i = 0; i < el.length;) {
					let sign = 1;
					let curr = decodeBase64(el.charCodeAt(i++));
					let cont = (curr & 0x20) === 0x20;
					if ((curr & 1) === 1) sign = -1;
					let res = (curr & 0b11110) >> 1;
					let n = 4;

					for (; i < el.length && cont;) {
						curr = decodeBase64(el.charCodeAt(i++));
						cont = (curr & 0x20) == 0x20;
						res |= (curr & 0b11111) << n;
						n += 5;
						if (!cont) break;
					}

					list.push(res * sign);
				}

				elements.push(list);
			}
		}

		lines.push(elements);
	}

	return lines;
}

export namespace Location {
	export function compare(a: Location, b: Location) {
		const { 0: filenameA, 1: lineA, 2: startA } = a;
		const { 0: filenameB, 1: lineB, 2: startB } = b;

		if (filenameA < filenameB) return -1;
		if (filenameA > filenameB) return 1;

		const lineI = lineA - lineB;
		if (lineI !== 0) return lineI;

		return startA - startB;
	}
	export function comparePoints(a: Location, b: Location) {
		const { 1: lineA, 2: startA } = a;
		const { 1: lineB, 2: startB } = b;

		const lineI = lineA - lineB;
		if (lineI !== 0) return lineI;

		return startA - startB;
	}
}

export class VLQSourceMap {
	public constructor(
		public readonly array: Map<string, [start: number, dst: Location][][]>,
	) { }

	public converter() {
		return (src: Location) => {
			const file = this.array.get(src[0]);
			if (file == null) return src;

			const line = file[src[1]];
			if (line == null || line.length === 0) return undefined;

			let a = 0;
			let b = line.length;

			while (true) {
				const done = b - a <= 1;

				const mid = (a + b) >> 1;
				const el = line[mid];

				const cmp = el[0] - src[2];

				if (cmp < 0) {
					if (done) {
						if (b >= line.length) return undefined;
						break;
					}
					a = mid;
				}
				else if (cmp > 0) {
					if (done) {
						if (a <= 0) return undefined;
						break;
					}
					b = mid;
				}
				else return el[1];
			}

			return line[b][1];
		};
	}

	public static parseVLQ(compiled: string, filenames: string[], raw: string): VLQSourceMap {
		const mapping = decodeVLQ(raw);
		const file: [start: number, dst: Location][][] = [];
		const res = new Map<string, [start: number, dst: Location][][]>([[compiled, file]]);

		let originalRow = 0;
		let originalCol = 0;
		let originalFile = 0;

		for (let compiledRow = 0; compiledRow < mapping.length; compiledRow++) {
			const line: [start: number, dst: Location][] = file[compiledRow] = [];
			let compiledCol = 0;

			const rowData = mapping[compiledRow];
			for (let i = 0; i < rowData.length; i++) {
				const rawSeg = rowData[i];

				compiledCol += rawSeg.length > 0 ? rawSeg[0] : 0;
				originalFile += rawSeg.length > 1 ? rawSeg[1] : 0;
				originalRow += rawSeg.length > 2 ? rawSeg[2] : 0;
				originalCol += rawSeg.length > 3 ? rawSeg[3] : 0;

				line[line.length] = [compiledCol, [filenames[originalFile], originalRow, originalCol]];
			}

			line.sort((a, b) => a[0] - b[0]);
		}

		return new VLQSourceMap(res);
	}
	public static parse(raw: string | { file: string, mappings: string, sources: string[] }) {
		if (typeof raw === "string") raw = JSON.parse(raw) as { file: string, mappings: string, sources: string[] };

		const compiled = raw.file;
		const mapping = raw.mappings;
		let filenames = raw.sources;
		if (filenames.length === 0 || filenames.length === 1 && filenames[0] === "") filenames = [compiled];

		return this.parseVLQ(compiled, filenames, mapping);
	}
}

export namespace SourceMap {
	export function parse(raw: string | { file: string, mappings: string, sources: string[] }) {
		return VLQSourceMap.parse(raw).converter();
	}
	export function chain(...maps: SourceMap[]): SourceMap {
		return loc => {
			for (const el of maps) {
				const tmp = el(loc);
				if (tmp == null) return undefined;
				else loc = tmp;
			}

			return loc;
		};
	}
}
