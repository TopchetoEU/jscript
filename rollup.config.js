const { defineConfig } = require("rollup");
const terser = require("@rollup/plugin-terser");
const typescript = require("@rollup/plugin-typescript");
const babel = require("@rollup/plugin-babel");
const commonjs = require("@rollup/plugin-commonjs");
const nodeResolve = require("@rollup/plugin-node-resolve");
const { resolve } = require("path");

const shouldMinify = () => false;
const shouldEmitSourcemaps = () => true;
const shouldPolyfill = () => !!process.env.POLYFILLS;

const construct = (input, output) => defineConfig({
	input,
	plugins: [
		shouldPolyfill() && {
			name: "babel-fake-runtime",
			resolveId(source) {
				if (source.startsWith("!polyfills:/helpers")) return resolve(process.env.POLYFILLS) + source.slice(19) + ".js";
			}
		},
		commonjs(),
		nodeResolve(),
		babel({
			extensions: [],
			exclude: ["node_modules/**"],

			babelHelpers: "runtime",
			plugins: [
				["@babel/plugin-transform-typescript", {
					onlyRemoveTypeImports: true,
					optimizeConstEnums: true,
					allowDeclareFields: true,
				}],
				["@babel/plugin-transform-class-properties"],
				["@babel/plugin-transform-runtime", {
					moduleName: shouldPolyfill() ? "!polyfills:" : undefined,
					version: "^7.24.0",
				}],
			]
		}),
		babel({
			extensions: [],
			exclude: shouldPolyfill() ? [process.env.POLYFILLS + "/**"] : [],

			assumptions: {
				ignoreToPrimitiveHint: true,
				noClassCalls: true,
			},

			env: {
				development: { compact: false },
			},

			babelHelpers: "runtime",
			plugins: [
				"@babel/plugin-transform-arrow-functions",
				"@babel/plugin-transform-block-scoping",
				"@babel/plugin-transform-classes",
				"@babel/plugin-transform-computed-properties",
				"@babel/plugin-transform-destructuring",
				"@babel/plugin-transform-for-of",
				"@babel/plugin-transform-object-super",
				"@babel/plugin-transform-parameters",
				"@babel/plugin-transform-shorthand-properties",
				"@babel/plugin-transform-spread",
				"@babel/plugin-transform-object-rest-spread",
				"@babel/plugin-transform-template-literals",
				"@babel/plugin-transform-unicode-escapes",
				"@babel/plugin-transform-unicode-regex",
				"@babel/plugin-transform-exponentiation-operator",
				"@babel/plugin-transform-async-to-generator",
				"@babel/plugin-transform-async-generator-functions",
				"@babel/plugin-transform-nullish-coalescing-operator",
				"@babel/plugin-transform-optional-chaining",
				"@babel/plugin-transform-logical-assignment-operators",
				"@babel/plugin-transform-numeric-separator",
				"@babel/plugin-transform-class-properties",
				"@babel/plugin-transform-class-static-block",
				"@babel/plugin-transform-regenerator",

				["@babel/plugin-transform-runtime", {
					moduleName: shouldPolyfill() ? "!polyfills:" : undefined,
					version: "^7.24.0",
				}],
			],
		}),
		typescript({
			exclude: ["node_modules/**", "*.js"],
			compilerOptions: {
				allowImportingTsExtensions: true,
				noEmit: true,
			},
			noForceEmit: true,
			noEmitOnError: true,
		}),
		shouldMinify() && terser({
			sourceMap: shouldEmitSourcemaps(),
			keep_classnames: true,
		}),
	],
	output: {
		file: output,
		format: "iife",
		globals: {
			fs: "null",
			path: "null",
			os: "null",
			inspector: "null",
		},
		// plugins: [babel.getBabelOutputPlugin({
		// 	allowAllFormats: true,
		// })],

		sourcemap: shouldEmitSourcemaps(),
		inlineDynamicImports: true,
	},
});

module.exports = construct(process.env.INPUT, process.env.OUTPUT);
