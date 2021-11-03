<template>
<div style="height: 100%">    
    <work-space />
</div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import WorkSpace from "@/components/WorkSpace/WorkSpace.vue"

@Component({
    components: {
        WorkSpace
    },
    beforeCreate() { 
    }
})

export default class WorkSpaceView extends Vue {

    constructor() {
        super();
    }
}
</script>