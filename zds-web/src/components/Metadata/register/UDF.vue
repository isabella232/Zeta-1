<template>
  <div class="udf-register-wrapper">
    <el-breadcrumb separator-class="el-icon-arrow-right">
      <el-breadcrumb-item>
        <span @click="backToMain">Metadata</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item>Register UDF</el-breadcrumb-item>
    </el-breadcrumb>

    <div
      v-loading="loading"
      class="form-wrapper"
    >
      <el-form
        ref="form"
        :model="form"
        :rules="rules"
        label-width="160px"
        label-position="left"
      >
        <el-form-item
          label="Name"
          prop="name"
        >
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item
          label="Owner"
          prop="owner"
        >
          <user-selection
            :nt="form.owner"
          />
        </el-form-item>
        <el-form-item
          label="Database"
          prop="db_name"
        >
          <el-input v-model="form.db_name" />
        </el-form-item>
        <el-form-item
          label="Class Name"
          prop="class_name"
        >
          <el-input v-model="form.class_name" />
        </el-form-item>
        <el-form-item
          label="Team DL"
          prop="team_dl"
        >
          <el-input v-model="form.team_dl" />
        </el-form-item>
        <el-form-item
          label="Platform"
          prop="platform"
        >
          <el-select
            v-model="form.platform"
            multiple
            placeholder="please select platform"
            style="width:500px"
            @change="onPlatformChange"
          >
            <el-option-group
              :disabled="isRealtime"
            >
              <el-option
                label="Apollo RNO"
                value="Apollo_rno"
              />
              <el-option
                label="Ares"
                value="Ares"
              />
              <!-- el-option
                label="Hopper"
                value="Hopper"
              /-->
              <el-option
                label="Hermes"
                value="Hermes"
              />
              <el-option
                label="Hercules"
                value="Hercules"
              />
            </el-option-group>
            <el-option-group
              :disabled="form.platform.length > 0 && !isRealtime"
            >
              <el-option
                label="Rheos"
                value="Rheos"
              />
            </el-option-group>
          </el-select>
        </el-form-item>

        <el-form-item
          label="Parameters"
          prop="parameters"
        >
          <el-input v-model="form.parameters" />
        </el-form-item>
        <el-form-item
          label="Description"
          prop="description"
        >
          <el-input
            v-model="form.description"
            type="textarea"
          />
        </el-form-item>
        <el-form-item
          label="Example"
          prop="example"
        >
          <el-input
            v-model="form.example"
            type="textarea"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="onSubmit"
          >
            Create
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script lang="ts">
/* eslint-disable @typescript-eslint/camelcase */
import { Vue, Component } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import UserSelection from '@/components/common/UserSelection.vue';
import Util from '@/services/Util.service';
import MetadataService from '@/services/remote/Metadata';

const api = DoeRemoteService.instance;

@Component({
  components: {
    UserSelection,
  },
})
export default class RegisterUDF extends Vue {

  metadataService: MetadataService = new MetadataService();
  loading = false;
  form = {
    'db_name': 'default',
    'name': '',
    'owner': Util.getNt(),
    'platform': [] as string[],
    'class_name': '',
    'description': '',
    'parameters': '',
    'example': '',
    'team_dl': '',
  };

  rules= {
    name: [
      { required: true, message: 'Please input UDF name', trigger: 'blur' },
    ],
    db_name: [
      { required: true, message: 'Please input database name', trigger: 'blur' },
    ],
    class_name: [
      { required: true, message: 'Please input class name', trigger: 'blur' },
    ],
    team_dl: [
      { required: true, message: 'Please input team DL', trigger: 'blur' },
      { type: 'email', message: 'Please input valid email address', trigger: 'blur' },
    ],
    platform: [
      { required: true, message: 'Please choose at lease one platform', trigger: 'blur' },
    ],
  };

  get isRealtime() {
    return !!this.form.platform.find(p => p === 'Rheos');
  }
  backToMain() {
    this.$router.push('/metadata');
  }

  onPlatformChange(selected: string[]) {
    const last = selected[selected.length - 1];
    if (last === 'Rheos') {
      this.form.platform = [last];
    } else {
      this.form.platform = this.form.platform.filter(p => p !== 'Rheos');
    }
  }

  async onSubmit() {
    (this.$refs.form as any).validate(async (isValid) => {
      if (isValid) {
        this.loading = true;
        try {
          await api.createUDF(this.form);
          this.$message.success('Your UDF is successfully registered');
          this.metadataService.createESUDF(this.form);
          this.backToMain();
        } catch (error) {
          console.error(error);
          this.$message.error('Ooops..' + error.message);
        } finally {
          this.loading = false;
        }
      } else {
        return false;
      }
    });
  }
}
</script>
<style lang="scss" scoped>
.udf-register-wrapper {
  padding: 30px 20px;
  .el-breadcrumb__item {
    &:not(:last-of-type) {
      cursor: pointer;
      span {
        color: #4d8cca;
        &:hover {
          text-decoration: underline;
        }
      }
    }
  }
  .form-wrapper {
    padding: 48px 0;
    width: 50vw;
  }
}
</style>
