import AutoELService from '@/services/remote/AutoELService';
const API = AutoELService.instance;

class SuggestionService {

  types = new Map<string, string>();
  names = new Map<string, string>();

  private async getFromCache(keys: string | string [], type: 'name' | 'type') {
    if (typeof keys === 'string') {
      keys = [ keys ];
    }
    let queryFunc, cache; 
    if (type === 'name') {
      cache = this.names;
      queryFunc = API.getSuggestedNames.bind(API);
    } else {
      cache = this.types;
      queryFunc = API.getSuggestedTypes.bind(API);
    }
    const result = {};
    const notFound: string[] = [];
    keys.forEach(n => {
      if (cache.has(n)) {
        result[n] = cache.get(n);
      } else {
        notFound.push(n);
      }
    });
    if (notFound.length > 0) {
      const resp = await queryFunc(notFound);
      resp.forEach(({ key, value }) => {
        cache.set(key, value);
        result[key] = value;
      });
    }
    return result;
  }

  async getSuggestedName(names: string | string[]) {
    return await this.getFromCache(names, 'name');
  }

  async getSuggestedType(names: string | string[]) {
    return await this.getFromCache(names, 'type');
  }
}

export default new SuggestionService();