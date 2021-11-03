import { parseName } from "@/services/zeppelin/Zeppelin.service";
import { expect } from 'chai';
describe('zeppelin.service', () => {
    it('parseName', () => {
        const name1 = 'root';
        expect(parseName('root')).deep.equal({name: 'root', path: '/'});
        expect(parseName('/root')).deep.equal({name: 'root', path: '/'})
        expect(parseName('/folder/file')).deep.equal({name: 'file', path: '/folder/'})
        expect(parseName('folder/file')).deep.equal({name: 'file', path: '/folder/'})
        
    })
})