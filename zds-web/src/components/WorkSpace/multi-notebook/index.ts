// {
//     "SPARK SQL": "text/x-mysql",
//     "Scala": "text/x-scala",
//     "R": "text/x-rsrc",
//     "Python": "text/x-python"
// }
export interface Lang {
    mime: 'text/x-mysql' | "text/x-rsrc" | 'text/x-python',
    interpreter: string,
    // codeType: 'sql' | 'sparkr' | 'pyspark'
    codeType: 'sql' | 'pyspark'
}
export const langMap: Dict<Lang> = {
    "SPARK SQL": {
        mime:"text/x-mysql",
        interpreter:"livy-shared",
        codeType: 'sql'
    },
    "Python": {
        mime:"text/x-python",
        interpreter:"livy-shared",
        codeType: 'pyspark'
    }
}