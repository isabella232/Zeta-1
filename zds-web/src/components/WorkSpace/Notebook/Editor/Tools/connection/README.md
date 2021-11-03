# How to add a new interpretor connection

## 1. add code type 
code path: src/types/workspace/notebook.internal.ts
add CodeType & CodeTypes
```
export enum CodeType {
	SQL = 'SQL',
	SCALA = 'SCALA',
	TERADATA = 'TERADATA',
+   R = 'R' 
}
```
```
export const CodeTypes: Dict<CodeTypeInfo> = {
	[CodeType.SQL]: {
		name: 'SPARK SQL',
		mime: 'text/x-mysql',
		interpreter: 'livy-sparksql'
	},
	[CodeType.TERADATA]: {
		name: 'TD SQL',
		mime: 'text/x-mysql',
		interpreter: 'jdbc',
		jdbcType: 'teradata'
	},
+    [CodeType.R]: {
+		name: 'R',
+		mime: 'text/x-r',
+		interpreter: 'XXX',
+	},
};
```
## 2. add connection adaptee
code path: src/components/WorkSpace/Notebook/Editor/Tools/connection/connection-by-plat

## 3. add an alias in Repo 


