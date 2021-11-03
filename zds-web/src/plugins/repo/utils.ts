import Vue from "vue";
import RepoApiHelper from "@/services/repo-api";

export const removeComponent = ($instance: Vue) => {
    $instance.$off('onSuccess');
    $instance.$off('onCancel');
    $instance.$off('onError');
    $instance.$destroy()
    document.body.removeChild($instance.$el);
}
export function mountComponent<T>(component: typeof Vue, repoApi?:RepoApiHelper, options?: T){
    const componentConstructor = Vue.extend(component);
    const $instance = new componentConstructor({data: {repoApi, ...options}});
    $instance.$mount();
    document.body.appendChild($instance.$el)
    return $instance;
}