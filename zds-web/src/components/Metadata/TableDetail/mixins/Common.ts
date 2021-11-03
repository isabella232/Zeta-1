import { Component, Vue } from 'vue-property-decorator';
@Component
export class Common extends Vue {
  
  get tableName () {
    return this.$route.params.name;
  }

}