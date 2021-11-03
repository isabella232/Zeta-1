import sqlFormatter from '@/services/sql-formatter/sqlFormatter'
describe('format SQL', () => {
    it('check params', () => {
        const formatSQL = (sql:string)=> {
            return sqlFormatter.format(sql, {
                language: 'sparksql'
            })
        }
        const SQL = 'SELECT ${aaa} FROM people;';
        console.info(formatSQL(SQL));
    })
})