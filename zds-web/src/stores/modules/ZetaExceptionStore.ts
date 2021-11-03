import { StoreOptions } from 'vuex'
import { ZetaException, ZETA_EXCEPTION_TAG, ExceptionPacket } from '@/types/exception';
import * as MUT from '../MutationTypes';
import { WSPacket } from '@/types/WSPackets';
import uuid from'uuid';
let e = new ZetaException(<ExceptionPacket> {
    code: 'any',
    errorDetail: {
        message: '[Test] notebook exception',
        cause: {
            stackTrace: [
                {
                  "methodName": "exception",
                  "fileName": "FooController.java",
                  "lineNumber": 26,
                  "className": "com.ebay.dss.zds.rest.FooController",
                  "nativeMethod": false
                },
                {
                  "methodName": "invoke0",
                  "fileName": "NativeMethodAccessorImpl.java",
                  "lineNumber": -2,
                  "className": "sun.reflect.NativeMethodAccessorImpl",
                  "nativeMethod": true
                }
            ],
            cause: ''
        }
    }
}, {
    path: 'notebook',
    workspaceId: 'c83a7fa1-e653-4b70-bad7-0d74058d5f15'
})
let e2 = new ZetaException(<ExceptionPacket> {
    code: 'any',
    errorDetail: {
        message: '[Test] REST ERROR in Repo 123123123123123123123123123132321 555555555555',
        cause: {
            cause: ''
        }
    }
}, {
    path: 'repository'
})

const options: StoreOptions<any> = {
    state: [] as ZetaException[],
    // state: [e,e2],
    mutations: {
        [MUT.EXC_ADD_EXCEPTION] (state: ZetaException[], { exception }) {
            if(Array.isArray(exception)){
                for(let e of exception) {
                    state.push(e)
                }
            } else {
                state.push(exception)
            }
        },
        [MUT.EXC_READ_EXCEPTION] (state: ZetaException[], {id}) {
            let e = state.find(e => e.id === id);
            if(e) {
                e.resolved = true;
            }
        }
    },
    getters: {
        zetaExceptions: function (state: ZetaException[]) {
            return state.filter(e => !e.resolved)
        }
    },
    actions: {
        addException({commit}, {exception}) {
            commit(MUT.EXC_ADD_EXCEPTION, {exception})
        }
    }
}

export default options;