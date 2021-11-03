<template>
<iframe :src="iframeURL" style="height: 100%;width:100%;"></iframe>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

@Component({
    components: {
      DoeSearch
    },
    beforeCreate() { 
    }
})

export default class DoeSearch extends Vue {

    constructor() {
        super();
    }

    get iframeURL (){
        return this.$store.getters.iframeURL ? this.$store.getters.iframeURL:"https://doe.corp.ebay.com/assets"
    }

    mounted () {
      // this.iframeURL = this.$store.getters.iframeURL
    }
}
</script>