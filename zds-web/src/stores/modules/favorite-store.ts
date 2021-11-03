import { StoreOptions } from 'vuex';
export interface FavoriteItemKey {
  id: string;
  favoriteType: 'nb' | 'pub_nb' | 'share_nb' | 'share_zpl_nb' | 'schedule';
}
export interface FavoriteItem extends FavoriteItemKey{
  favorite: boolean;
}
const option: StoreOptions<any> = {
  state: {
    favoriteList: [] as FavoriteItem[],
  },
  mutations: {
    initFavoriteList (state, favoriteList){
      favoriteList.forEach((item: FavoriteItem) => {
        state.favoriteList.push(item);
      });
    },
    favorite( state, favorite: FavoriteItemKey) {
      const item = (state.favoriteList as FavoriteItem[])
        .find(item => item.favoriteType === favorite.favoriteType && item.id === favorite.id);
      if (item) {
        item.favorite = true;
      } else {
        state.favoriteList.push({
          ...favorite,
          favorite: true,
        });
      }
    },
    unfavorite( state, favorite: FavoriteItemKey) {
      const item = (state.favoriteList as FavoriteItem[])
        .find(item => item.favoriteType === favorite.favoriteType && item.id === favorite.id);
      if (item) {
        item.favorite = false;
      } else {
        state.favoriteList.push({
          ...favorite,
          favorite: false,
        });
      }
    },
  },
  getters: {
    getFavoriteById: (state) => {
      return (id: string, type: 'nb' | 'pub_nb' | 'share_nb' | 'share_zpl_nb' | 'schedule') => {
        const favorite =  state.favoriteList.find((item: FavoriteItem) => item.favoriteType === type && item.id == id);
        if (!favorite) {
          return false;
        }
        return favorite.favorite;
      };
    },
    favoriteNotebooks: state => state.favoriteList.filter((item: FavoriteItem) => item.favoriteType === 'nb'),
    favoritePublicNotebooks: state => state.favoriteList.filter((item: FavoriteItem) => item.favoriteType === 'pub_nb'),
    favoriteSchedules: state => state.favoriteList.filter((item: FavoriteItem) => {
      return (item.favoriteType === 'schedule') && item.favorite;
    }),
  },
  actions: {
    initFavoriteList({ commit }, favoriteList) {
      commit('initFavoriteList', favoriteList);
    },
    favorite({ commit }, item: FavoriteItemKey) {
      commit('favorite', item);
    },
    unfavorite({ commit }, item: FavoriteItemKey) {
      commit('unfavorite', item);
    },
  },
};
export default option;
